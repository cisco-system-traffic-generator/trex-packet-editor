package com.xored.javafx.packeteditor.events;

import com.xored.javafx.packeteditor.scapy.ScapyPkt;

public class ReloadModelEvent {
    private ScapyPkt pkt;

    public ReloadModelEvent(ScapyPkt pkt) {
        this.pkt = pkt;
    }

    public ScapyPkt getPkt() {
        return pkt;
    }
}
