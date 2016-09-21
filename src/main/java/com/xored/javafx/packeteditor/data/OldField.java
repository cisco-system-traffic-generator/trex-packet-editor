package com.xored.javafx.packeteditor.data;

import java.util.List;

public class OldField implements IField {
    private String id;
    private String name;
    private int offset;
    private int globalOffset;
    private int length;
    private Type type;
    private String value;
    private List<String> path;
    FieldEditHandler onSetValue;

    public interface FieldEditHandler {
        void operation(String value);
    }

    public OldField(String id, int offset, int length, int globalOffset, String value, Type type) {
        this.id = id;
        this.name = id;
        this.offset = offset;
        this.globalOffset = globalOffset;
        this.length = length;
        this.type = type;
        this.value = value;
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

    public String getId() { return id; }

    public String getName() {
        return name;
    }
    public String getDisplayValue() { return value; }

    public void setOnSetValue(FieldEditHandler onSetValue) { this.onSetValue = onSetValue; }

    public void setStringValue(String value) {
        if (onSetValue != null) {
            onSetValue.operation(value);
        }
    }

    public void setPath(List<String> currentPath) { this.path = currentPath; }
    public List<String> getPath() { return path; }

    public Type getType() {
        return type;
    }


}
