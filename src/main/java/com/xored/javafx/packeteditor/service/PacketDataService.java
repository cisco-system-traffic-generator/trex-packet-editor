package com.xored.javafx.packeteditor.service;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.events.ScapyClientConnectedEvent;
import com.xored.javafx.packeteditor.scapy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.scapy.ScapyUtils.createReconstructPktPayload;

public class PacketDataService {
    static Logger logger = LoggerFactory.getLogger(PacketDataService.class);
    
    @Inject
    ScapyServerClient scapy;
    
    private boolean initialized = false;

    @Subscribe
    public void handleScapyConnectedEvent(ScapyClientConnectedEvent event) {
        initialized = true;
    }
    
    public PacketData buildPacket(List<ReconstructProtocol> pktStructure) {
        return scapy.build_pkt(pktStructure);
    }
    
    public PacketData buildPacket(List<ReconstructProtocol> pktStructure, JsonElement extra_options) {
        if (extra_options == null) {
            return buildPacket(pktStructure);
        } else {
            return scapy.build_pkt_ex(pktStructure, extra_options);
        }
    }
    
    public PacketData reconstructPacket(PacketData currentPkt, List<ReconstructProtocol> modify) {
        return scapy.reconstruct_pkt(currentPkt.getPacketBytes(), modify);
    }

    public PacketData reconstructPacketField(PacketData currentPkt, List<String> path, ReconstructField newValue) {
        return reconstructPacket(currentPkt, createReconstructPktPayload(path, newValue));
    }

    public PacketData reconstructPacketFromBinary(byte[] bytes) {
        return scapy.reconstruct_pkt(bytes);
    }

    public FieldData getRandomFieldValue(String protocolId, String fieldId) {
        PacketData pd = scapy.build_pkt(Arrays.asList(ReconstructProtocol.modify(
                protocolId,
                Arrays.asList(ReconstructField.randomizeValue(fieldId))
        )));
        return pd.getProtocols().get(0).getFieldById(fieldId);
    }

    /** appends protocol to the stack */
    public PacketData appendProtocol(PacketData currentPkt, String protocolId) {
        if (currentPkt == null) {
            return scapy.build_pkt(Arrays.asList(ReconstructProtocol.pass(protocolId)));
        }
        List<ReconstructProtocol> modify = currentPkt.getProtocols().stream().map(protocol ->
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
    public PacketData removeLastProtocol(PacketData pkt) {
        List<ReconstructProtocol> protocols = pkt.getProtocols().stream().map(protocol ->
                ReconstructProtocol.pass(protocol.id)
        ).collect(Collectors.toList());

        if (protocols.size() > 1)  {
            protocols.get(protocols.size() - 1).delete = true;
            return reconstructPacket(pkt, protocols);
        } else {
           return new PacketData();
        }
    }

    public byte[] write_pcap_packet(byte[] binaryData) {
        return scapy.write_pcap_packet(binaryData);
    }

    public PacketData read_pcap_packet(byte[] binaryData) {
        return scapy.read_pcap_packet(binaryData);
    }

    public void closeConnection() {
        scapy.closeConnection();
    }

    public boolean isInitialized() {
        return initialized;
    }
}
