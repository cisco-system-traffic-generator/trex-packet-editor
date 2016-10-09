package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

import java.util.ArrayList;
import java.util.List;

public class ScapyProtocol {
    
    private ProtocolMetadata meta;

    private List<String> path;

    private final String id;

    private final String name;

    private final List<ScapyField> fields;

    public ScapyProtocol(ProtocolMetadata meta, List<String> path) {
        this.meta = meta;
        this.path = path;
        this.id = meta.getId();
        this.name = meta.getName();
        this.fields = new ArrayList<>();
    }

    public ProtocolMetadata getMeta() {
        return meta;
    }

    public List<String> getPath() {
        return path;
    }

    public List<ScapyField> getFields() {
        return fields;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
    
    
}
