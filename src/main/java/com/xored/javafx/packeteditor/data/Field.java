package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.FieldMetadata;

import java.util.List;

public class Field implements IField {
    private FieldMetadata meta;

    private int offset;
    private int globalOffset;
    private int length;
    private String value;
    public Field(FieldMetadata meta, int offset, int length, int globalOffset) {
        this.meta = meta;
        this.offset = offset;
        this.globalOffset = globalOffset;
        this.length = length;
    }

    public Field(FieldMetadata meta, int offset, int length, int globalOffset, String value) {
        this.meta = meta;
        this.offset = offset;
        this.globalOffset = globalOffset;
        this.length = length;
        this.value = value;
    }

    public FieldMetadata getMeta() {
        return meta;
    }

    public int getOffset() {
        return offset;
    }
    
    public int getAbsOffset() {
        return globalOffset + offset;
    }

    public int getLength() {
        return length;
    }

    public String getId() { return meta.getId(); }

    public String getName() {
        return meta.getName();
    }
    
    public String getDisplayValue() { return value == null ? "Not set" : value; }

    public Type getType() {
        return meta.getType();
    }

    public void setStringValue(String value) { }

    public void setPath(List<String> currentPath) { }

    public List<String> getPath() { return null; }
}
