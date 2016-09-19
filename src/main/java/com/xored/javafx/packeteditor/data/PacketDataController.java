package com.xored.javafx.packeteditor.data;

import com.google.gson.JsonArray;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;

import java.util.Observable;

public class PacketDataController extends Observable {
    @Inject ScapyServerClient scapy;
    @Inject IBinaryData binary;

    ScapyPkt pkt;

    public void init() {
        scapy.open("tcp://localhost:4507");
        read_pkt(scapy.getHttpPkt());
    }

    public void read_pkt(ScapyPkt payload) {
        pkt = payload;
        byte [] bytes = pkt.getBinaryData();
        setChanged();
        notifyObservers(null);
        binary.setBytes(bytes);
    }

    public JsonArray getProtocols() {
        return pkt.getProtocols();
    }

}
