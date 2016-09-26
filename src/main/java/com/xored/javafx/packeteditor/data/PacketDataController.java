package com.xored.javafx.packeteditor.data;

import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.controllers.PacketUndoController;
import com.xored.javafx.packeteditor.events.ReloadModelEvent;
import com.xored.javafx.packeteditor.scapy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.scapy.ScapyUtils.createReconstructPktPayload;

import static com.xored.javafx.packeteditor.data.IField.*;

public class PacketDataController extends Observable {
    static Logger log = LoggerFactory.getLogger(PacketDataController.class);
    final Gson gson = new Gson();
    @Inject
    ScapyServerClient scapy;

    File currentFile;
    
    @Inject
    EventBus eventBus;

    @Inject
    PacketUndoController undoController;

    ScapyPkt pkt = new ScapyPkt();

    public void init() {
        scapy.open("tcp://localhost:4507");
    }


    public File getCurrentFile() { return currentFile; }

    // TODO: reimplement it as service and make it stateless,
    public void replacePacket(ScapyPkt payload) {
        undoController.beforeContentReplace(pkt);
        pkt = payload;
        setChanged();
        notifyObservers(null); // deprecated
        eventBus.post(new ReloadModelEvent(pkt));
    }

    public JsonArray getProtocols() {
        return pkt.getProtocols();
    }

    public void newPacket() {
        currentFile = null;
        replacePacket(new ScapyPkt());
    }

    public void loadPcapFile(String filename) throws Exception {
        loadPcapFile(new File(filename));
    }

    public void loadPcapFile(File file) throws Exception {
        byte[] bytes = Files.toByteArray(file);
        currentFile = file;
        replacePacket(scapy.read_pcap_packet(bytes));
    }

    public void writeToPcapFile(File file) throws Exception {
        byte[] pcap_bin = scapy.write_pcap_packet(pkt.getBinaryData());
        Files.write(pcap_bin, file);
        currentFile = file;
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

    /** can accept string representation for strings, integers, hex, ... */
    public void setFieldValue(IField field, String newValue) {
        /* TODO: these special cases can be removed - used for testing */
        if (DEFAULT.equals(newValue)) {
            setFieldDefaultValue(field);
        } else if (RANDOM.equals(newValue)) {
            setFieldRandomValue(field);
        } else {
            setFieldValue(field.getPath(), field.getId(), newValue);
        }
    }

    /** @NotImplementedYet set binary payload to a field */
    public void setFieldValueBytes(IField field, byte[] bytes) {
        // TODO: implement properly
        setFieldValue(field, "<binary payload" + bytes.length + ">");
    }

    private void setFieldValue(List<String> fieldPath, String fieldName, String newValue) {
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
        List<ReconstructProtocol> modify = pkt.packet().getProtocols().stream().map(protocol ->
                ReconstructProtocol.pass(protocol.id)
        ).collect(Collectors.toList());
        if (protocolId.equals("Raw")) {
            // We need to have some dummy payload, or Scapy will remove it
            modify.add(ReconstructProtocol.modify("Raw", Arrays.asList(ReconstructField.setValue("load", "dummy"))));
        } else {
            modify.add(ReconstructProtocol.pass(protocolId));
        }
        reconstructPacket(modify);
    }

    /** removes inner protocol */
    public void removeLastProtocol() {
        List<ReconstructProtocol> protocols = pkt.packet().getProtocols().stream().map(protocol ->
                ReconstructProtocol.pass(protocol.id)
        ).collect(Collectors.toList());

        if (protocols.size() > 1)  {
            protocols.get(protocols.size() - 1).delete = true;
            reconstructPacket(protocols);
        } else {
           newPacket();
        }
    }

    /* Reset length and chksum fields
     * type fields can be calculated for layers with payload
     *  */
    public void recalculateAutoValues() {
        List<ProtocolData> protocols = pkt.packet().getProtocols();
        List<ReconstructProtocol> modify = protocols.stream().map(
                protocol -> {
                    boolean is_last_layer = protocol == protocols.get(protocols.size() - 1);
                    return ReconstructProtocol.modify(protocol.id, protocol.fields.stream().filter(
                            field -> {
                                boolean field_reset_required =
                                        field.id.equals("length") ||
                                        field.id.equals("chksum") ||
                                        (field.id.equals("type") && is_last_layer);
                                return field_reset_required;
                            }
                    ).map(f -> ReconstructField.resetValue(f.id)).collect(Collectors.toList()));
                }).collect(Collectors.toList());
        reconstructPacket(modify);
    }
}
