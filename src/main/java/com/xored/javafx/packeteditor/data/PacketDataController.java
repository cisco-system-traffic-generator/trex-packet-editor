package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.events.ReloadModelEvent;
import com.xored.javafx.packeteditor.scapy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.scapy.ScapyUtils.createReconstructPktPayload;

public class PacketDataController extends Observable {
    static Logger log = LoggerFactory.getLogger(PacketDataController.class);
    final Gson gson = new Gson();
    @Inject
    ScapyServerClient scapy;
    
    @Inject
    EventBus eventBus;
    
    ScapyPkt pkt;

    public void init() {
        scapy.open("tcp://localhost:4507");
//        ClassLoader classLoader = getClass().getClassLoader();
//        File example_file = new File(classLoader.getResource("http_get_request.pcap").getFile());
//        try {
//            loadPcapFile(example_file);
//        } catch (Exception e) {
//            log.error("{}", e);
//        }
    }
    
    
    // TODO: reimplement it as service and make it stateless,
    public void replacePacket(ScapyPkt payload) {
        pkt = payload;
        setChanged();
        notifyObservers(null); // deprecated
        eventBus.post(new ReloadModelEvent(pkt));
    }

    public JsonArray getProtocols() {
        return pkt.getProtocols();
    }

    public void newPacket() {
        replacePacket(new ScapyPkt());
    }

    public void loadPcapFile(String filename) throws Exception {
        loadPcapFile(new File(filename));
    }

    public void loadPcapFile(File file) throws Exception {
        byte[] bytes = Files.toByteArray(file);
        replacePacket(scapy.read_pcap_packet(bytes));
    }

    public void writeToPcapFile(File file) throws Exception {
        byte[] pcap_bin = scapy.write_pcap_packet(pkt.getBinaryData());
        Files.write(pcap_bin, file);
    }

    public void modifyPacketField(IField field, String newValue) {
        /* TODO: these special cases can be removed - used for testing */
        if (newValue.equals("clear()")) {
            setFieldDefaultValue(field);
        } else if (newValue.equals("rnd()")) {
            setFieldRandomValue(field);
        } else {
            modifyPacketField(field.getPath(), field.getId(), newValue);
        }
    }

    public void reconstructPacket(List<ReconstructProtocol> modify) {
        reconstructPacket(gson.toJsonTree(modify));
    }

    public void reconstructPacket(JsonElement modifyProtocols) {
        try {
            ScapyPkt newPkt = new ScapyPkt(scapy.reconstruct_pkt(pkt.getBinaryData(), modifyProtocols));
            replacePacket(newPkt);
        } catch (Exception e) {
            log.error("Can't modify: {}", e);
        }
    }

    public void reconstructPacketFromBinary(byte[] bytes) {
        try {
            ScapyPkt newPkt = new ScapyPkt(scapy.reconstruct_pkt(bytes, new JsonArray()));
            replacePacket(newPkt);
        } catch (Exception e) {
            log.error("Can't modify: {}", e);
        }
    }

    private void modifyPacketField(List<String> fieldPath, String fieldName, String newValue) {
        reconstructPacket(createReconstructPktPayload(fieldPath, fieldName, newValue, false, false));
    }

    public void setFieldRandomValue(IField field) {
        reconstructPacket(createReconstructPktPayload(field.getPath(), field.getId(), null, true, false));
    }

    public void setFieldDefaultValue(IField field) {
        reconstructPacket(createReconstructPktPayload(field.getPath(), field.getId(), null, false, true));
    }

    /** appends protocol to the stack */
    public void appendProtocol(String protocolId) {
        if (pkt == null) {
            JsonArray params = new JsonArray();
            params.add(ScapyUtils.layer(protocolId));
            replacePacket(new ScapyPkt(scapy.build_pkt(params)));
            return;
        }
        ReconstructPacketBuilder modifyBuilder = new ReconstructPacketBuilder();
        modifyBuilder.appendStructureFromPacket(pkt.getProtocols());
        modifyBuilder.appendProtocol(protocolId);
        reconstructPacket(modifyBuilder.getProtocols());
    }

    /** removes inner protocol */
    public void removeLastProtocol() {
        ReconstructPacketBuilder modifyBuilder = new ReconstructPacketBuilder();
        modifyBuilder.appendStructureFromPacket(pkt.getProtocols());
        modifyBuilder.markProtocolForDeletion();
        reconstructPacket(modifyBuilder.getProtocols());
    }

    /* Reset length and chksum fields
     * type fields can be calculated for layers with payload
     *  */
    public void recalculateAutoValues() {
        List<ProtocolData> protocols = pkt.packet().getProtocols();
        List<ReconstructProtocol> modify = protocols.stream().map(protocol -> (
            ReconstructProtocol.modify(protocol.id, protocol.fields.stream().filter(f->
                    f.id.equals("length") ||
                    f.id.equals("chksum") ||
                    (f.id.equals("type") && protocol == protocols.get(protocols.size() - 1))
            ).map( f-> ReconstructField.resetValue(f.id) ).collect(Collectors.toList())
        ))).collect(Collectors.toList());
        reconstructPacket(modify);
    }
}
