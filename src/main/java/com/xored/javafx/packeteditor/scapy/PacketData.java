package com.xored.javafx.packeteditor.scapy;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This is class is a result of build_pkt, reconstruct_pkt
 */
public class PacketData {
    public List<ProtocolData> data = new ArrayList<>();
    public String binary = ""; // binary packet data in base64 encoding

    public byte[] getPacketBytes() { return Base64.getDecoder().decode(binary); }
    public List<ProtocolData> getProtocols() { return data; }
}
