package com.xored.javafx.packeteditor.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.scapy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.data.IField.DEFAULT;
import static com.xored.javafx.packeteditor.data.IField.RANDOM;
import static com.xored.javafx.packeteditor.scapy.ScapyUtils.createReconstructPktPayload;

public class PacketDataController extends Observable {
    static Logger log = LoggerFactory.getLogger(PacketDataController.class);

    final Gson gson = new Gson();

    @Inject
    ScapyServerClient scapy;

    File currentFile;

    public void init() {
        scapy.open("tcp://localhost:4507");
    }


    public File getCurrentFile() { return currentFile; }

    public ScapyPkt reconstructPacket(ScapyPkt currentPkt, List<ReconstructProtocol> modify) {
        return reconstructPacket(currentPkt, gson.toJsonTree(modify));
    }

    public ScapyPkt reconstructPacket(ScapyPkt currentPkt, JsonElement modifyProtocols) {
        return new ScapyPkt(scapy.reconstruct_pkt(currentPkt.getBinaryData(), modifyProtocols));
    }

    public ScapyPkt reconstructPacketFromBinary(byte[] bytes) {
        return new ScapyPkt(scapy.reconstruct_pkt(bytes, new JsonArray()));
    }

    /** can accept string representation for strings, integers, hex, ... */
    public ScapyPkt setFieldValue(ScapyPkt currentPkt, IField field, String newValue) {
        ScapyPkt result;
        if (DEFAULT.equals(newValue)) {
            result = setFieldDefaultValue(currentPkt, field);
        } else if (RANDOM.equals(newValue)) {
            result = setFieldRandomValue(currentPkt, field);
        } else {
            result = setFieldValue(currentPkt, field.getPath(), field.getId(), newValue);
        }
        return result;
    }

    /** @NotImplementedYet set binary payload to a field */
    public ScapyPkt setFieldValueBytes(ScapyPkt currentPkt, IField field, byte[] bytes) {
        // TODO: implement properly
        return setFieldValue(currentPkt, field, "<binary payload" + bytes.length + ">");
    }

    private ScapyPkt setFieldValue(ScapyPkt currentPkt, List<String> fieldPath, String fieldName, String newValue) {
        return reconstructPacket(currentPkt, createReconstructPktPayload(fieldPath, fieldName, newValue, false, false));
    }

    public ScapyPkt setFieldRandomValue(ScapyPkt currentPkt, IField field) {
        return reconstructPacket(currentPkt, createReconstructPktPayload(field.getPath(), field.getId(), null, true, false));
    }

    public ScapyPkt setFieldDefaultValue(ScapyPkt currentPkt, IField field) {
        return reconstructPacket(currentPkt, createReconstructPktPayload(field.getPath(), field.getId(), null, false, true));
    }

    /** appends protocol to the stack */
    public ScapyPkt appendProtocol(ScapyPkt currentPkt, String protocolId) {
        if (currentPkt == null) {
            JsonArray params = new JsonArray();
            params.add(ScapyUtils.layer(protocolId));
            return new ScapyPkt(scapy.build_pkt(params));
        }
        List<ReconstructProtocol> modify = currentPkt.packet().getProtocols().stream().map(protocol ->
                ReconstructProtocol.pass(protocol.id)
        ).collect(Collectors.toList());
        if (protocolId.equals("Raw")) {
            // We need to have some dummy payload, or Scapy will remove it
            modify.add(ReconstructProtocol.modify("Raw", Arrays.asList(ReconstructField.setValue("load", "dummy"))));
        } else {
            modify.add(ReconstructProtocol.pass(protocolId));
        }
        return reconstructPacket(currentPkt, modify);
    }

    /** removes inner protocol */
    public ScapyPkt removeLastProtocol(ScapyPkt pkt) {
        List<ReconstructProtocol> protocols = pkt.packet().getProtocols().stream().map(protocol ->
                ReconstructProtocol.pass(protocol.id)
        ).collect(Collectors.toList());

        if (protocols.size() > 1)  {
            protocols.get(protocols.size() - 1).delete = true;
            return reconstructPacket(pkt, protocols);
        } else {
           return new ScapyPkt();
        }
    }

    public byte[] write_pcap_packet(byte[] binaryData) {
        return scapy.write_pcap_packet(binaryData);
    }

    public ScapyPkt read_pcap_packet(byte[] binaryData) {
        return scapy.read_pcap_packet(binaryData);
    }
}
