package com.xored.javafx.packeteditor.data;

import com.google.inject.Inject;
import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;

public class PacketDataController {
    @Inject ScapyServerClient scapy;
    @Inject IBinaryData binary;

    public void init() {
        scapy.open("tcp://localhost:4507");
        ScapyPkt ethernetPkt = scapy.getHttpPkt();
        byte [] bytes = ethernetPkt.getBinaryData();
        binary.setBytes(bytes);
    }
}
