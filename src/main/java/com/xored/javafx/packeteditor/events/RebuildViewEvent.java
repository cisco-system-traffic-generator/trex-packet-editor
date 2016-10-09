package com.xored.javafx.packeteditor.events;

import com.xored.javafx.packeteditor.data.ScapyProtocol;

import java.util.Stack;

public class RebuildViewEvent {
    private Stack<ScapyProtocol> protocols;

    public RebuildViewEvent(Stack<ScapyProtocol> protocols) {
        this.protocols = protocols;
    }

    public Stack<ScapyProtocol> getProtocols() {
        return protocols;
    }
}
