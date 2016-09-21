package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

import java.util.List;
import java.util.stream.Collectors;

public class Protocol {
    
    public enum State {
        OPEN, COLLAPSED
    }

    private State state = State.OPEN;

    private ProtocolMetadata meta;

    private final String id;

    private final String name;

    private final List<Field> fields;

    public Protocol(ProtocolMetadata meta) {
        this.meta = meta;
        this.id = meta.getId();
        this.name = meta.getName();
        this.fields = meta.getFields().stream().map(metadata -> new Field(metadata, 0, 0, 0)).collect(Collectors.toList());
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
    
    
}
