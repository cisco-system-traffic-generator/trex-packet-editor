package com.xored.javafx.packeteditor.data.user;


import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Protocol {
    private ProtocolMetadata meta;
    private String id;
    private List<String> path = new ArrayList<>();
    private Map<String, Field> fieldMap = new LinkedHashMap<>();
    
    public Protocol(ProtocolMetadata meta, List<String> path) {
        this.meta = meta;
        this.id = meta.getId();
        this.path.addAll(path);
    }
    public void addField(Field field) {
        fieldMap.put(field.getId(), field);
    }

    public void addField(String fieldId, String value) {
        Field field = new Field(fieldId, path);
        field.setValue(value);
        fieldMap.put(field.getId(), field);
    }

    public Field getField(String fieldId) {
        return fieldMap.get(fieldId);
    }
    
    public ProtocolMetadata getMeta() {
        return meta;
    }

    public String getId() {
        return id;
    }

    public List<Field> getFields() {
        return fieldMap.entrySet().stream()
                .filter(entry -> entry.getValue().getValue() != null)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public Field createField(String fieldId) {
        Field field = new Field(fieldId, path);
        fieldMap.put(fieldId, field);
        return field;
    }
    
    public void deleteField(String fieldId) {
        fieldMap.put(fieldId, null);
    }
}
