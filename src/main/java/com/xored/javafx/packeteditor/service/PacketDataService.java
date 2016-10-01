package com.xored.javafx.packeteditor.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.IField;
import com.xored.javafx.packeteditor.scapy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.scapy.ScapyUtils.createReconstructPktPayload;

public class PacketDataService {
    static Logger log = LoggerFactory.getLogger(PacketDataService.class);

    final Gson gson = new Gson();

    @Inject
    ScapyServerClient scapy;

    public void init() {
        scapy.open("tcp://localhost:4507");
    }

    public ScapyPkt reconstructPacket(ScapyPkt currentPkt, List<ReconstructProtocol> modify) {
        return reconstructPacket(currentPkt, gson.toJsonTree(modify));
    }

    public ScapyPkt reconstructPacket(ScapyPkt currentPkt, JsonElement modifyProtocols) {
        return new ScapyPkt(scapy.reconstruct_pkt(currentPkt.getBinaryData(), modifyProtocols));
    }

    public ScapyPkt reconstructPacketFromBinary(byte[] bytes) {
        return new ScapyPkt(scapy.reconstruct_pkt(bytes, new JsonArray()));
    }

    public ScapyPkt setFieldValue(ScapyPkt currentPkt, IField field, ReconstructField newValue) {
        return reconstructPacket(currentPkt, createReconstructPktPayload(field.getPath(), newValue));
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
