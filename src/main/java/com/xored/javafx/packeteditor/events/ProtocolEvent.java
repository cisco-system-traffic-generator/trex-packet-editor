package com.xored.javafx.packeteditor.events;

import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

public class ProtocolEvent extends Event{
    
    private Action action;

    private ProtocolMetadata protocolMetadata;

    private Object value;

    public ProtocolEvent(Action action, ProtocolMetadata meta, Object value) {
        this.action = action;
        this.protocolMetadata = meta;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }
    
    public Object getValue() {
        return value;
    }

    public ProtocolMetadata getProtocolMetadata() {
        return protocolMetadata;
    }
}
