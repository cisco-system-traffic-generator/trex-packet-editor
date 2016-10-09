package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

import java.util.ArrayList;
import java.util.List;

public class ScapyProtocol {
    private final String id;
    private final String name;
    private final List<ScapyField> fields = new ArrayList<>();

    public ScapyProtocol(ProtocolMetadata meta, List<String> path) {
        this.id = meta.getId();
        this.name = meta.getName();
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
