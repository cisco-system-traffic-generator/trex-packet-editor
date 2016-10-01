package com.xored.javafx.packeteditor.metatdata;

import com.google.gson.internal.LinkedTreeMap;

import java.util.*;
import java.util.stream.Collectors;

public class ProtocolMetadata {
    private String id; // scapy class id
    private String name; // protocol name
    private LinkedTreeMap<String, FieldMetadata> fields = new LinkedTreeMap<>();
    private List<String> payload; // payload classes

    public ProtocolMetadata(String id, String name, List<FieldMetadata> fields, List<String> payload) {
        this.id = id;
        this.name = name;
        this.payload = payload;
        for(FieldMetadata fieldMeta : fields) {
           this.fields.put(fieldMeta.getId(), fieldMeta);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public List<FieldMetadata> getFields() {
        return fields.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public List<String> getPayload() {
        return payload;
    }

    public FieldMetadata getMetaForField(String fieldId) {
        return fields.get(fieldId);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
