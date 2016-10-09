package com.xored.javafx.packeteditor.data;

import com.google.gson.JsonElement;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScapyField implements IField {
    private boolean isDefault = true;
    private FieldMetadata meta;
    private FieldData field_data;
    private List<String> path = new ArrayList<>();
    private int globalOffset;

    FieldEditHandler onSetValue;
    public interface FieldEditHandler {
        void operation(String value);
    }
    public ScapyField(FieldMetadata meta, List<String> path, int globalOffset, FieldData field_data) {
        this.meta = meta;
        this.path.addAll(path);
        this.globalOffset = globalOffset;
        this.field_data = field_data;
    }
    public void setValue(JsonElement value) {
        this.field_data.value = value;
    }

    public FieldMetadata getMeta() {
        return meta;
    }
    public FieldData getData() { return field_data; }

    public int getOffset() {
        return field_data.getOffset();
    }
    
    public int getAbsOffset() {
        return globalOffset + getOffset();
    }

    public int getLength() {
        return field_data.getLength();
    }

    public String getId() { return meta.getId(); }

    public String getName() {
        return meta.getName();
    }

    public String getDisplayValue() { return field_data.hvalue; }
    
    public JsonElement getValue() { return field_data.value; }

    public Type getType() {
        return meta.getType();
    }

    /** returns protocol path of protocol id */
    public List<String> getPath() { return path; }

    /** returns last protocol in path */
    public String getProtocolId() { return path.get(path.size() - 1); }

    public String getUniqueId() {
        List<String> path = new ArrayList<>(this.path);
        path.add(getId());
        return path.stream().collect(Collectors.joining("-"));
    }

}
