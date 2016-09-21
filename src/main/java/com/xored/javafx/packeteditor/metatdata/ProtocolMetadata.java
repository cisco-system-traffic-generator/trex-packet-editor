package com.xored.javafx.packeteditor.metatdata;

import java.util.*;

public class ProtocolMetadata {
    private String id;
    private String name;
    private List<FieldMetadata> fields;
    private List<String> payload;

    public ProtocolMetadata(String id, String name, List<FieldMetadata> fields, List<String> payload) {
        this.id = id;
        this.name = name;
        this.payload = payload;
        this.fields = fields;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public List<FieldMetadata> getFields() {
        return fields;
    }

    public List<String> getPayload() {
        return payload;
    }

    public void addField(FieldMetadata field) {
        fields.add(field);
    }

    @Override
    public String toString() {
        return name;
    }
}
