package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

import java.util.ArrayList;
import java.util.List;

public class Protocol {
    
    public enum State {
        OPEN, COLLAPSED
    }

    private State state = State.OPEN;

    private ProtocolMetadata meta;

    private List<String> path;

    private final String id;

    private final String name;

    private final List<Field> fields;

    public Protocol(ProtocolMetadata meta, List<String> path) {
        this.meta = meta;
        this.path = path;
        this.id = meta.getId();
        this.name = meta.getName();
        this.fields = new ArrayList<>();
    }

    public State getState() {
        return state;
    }

    public ProtocolMetadata getMeta() {
        return meta;
    }

    public List<String> getPath() {
        return path;
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
