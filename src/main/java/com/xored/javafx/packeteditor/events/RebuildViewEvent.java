package com.xored.javafx.packeteditor.events;

import com.xored.javafx.packeteditor.data.Protocol;

import java.util.Stack;

public class RebuildViewEvent {
    private Stack<Protocol> protocols;

    public RebuildViewEvent(Stack<Protocol> protocols) {
        this.protocols = protocols;
    }

    public Stack<Protocol> getProtocols() {
        return protocols;
    }
}
