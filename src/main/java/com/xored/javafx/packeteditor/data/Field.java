package com.xored.javafx.packeteditor.data;

import com.google.gson.JsonElement;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;

import java.util.List;

import static com.xored.javafx.packeteditor.data.IField.Type.BITMASK;

public class Field implements IField {
    private FieldMetadata meta;
    private List<String> path;
    private int offset;
    private int globalOffset;
    private int length;
    private String hvalue;
    private JsonElement value;
    FieldEditHandler onSetValue;
    public interface FieldEditHandler {
        void operation(String value);
    }
    public Field(FieldMetadata meta, List<String> path, int offset, int length, int globalOffset, String hvalue, JsonElement value) {
        this.meta = meta;
        this.path = path;
        this.offset = offset;
        this.globalOffset = globalOffset;
        this.length = length;
        this.value = value;
        this.hvalue = hvalue;
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

    public void setOnSetCallback(FieldEditHandler onSetValue) { this.onSetValue = onSetValue; }
    
    public String getDisplayValue() { return hvalue; }
    
    public String getHValue() { return hvalue; }

    public JsonElement getValue() { return value; }

    public Type getType() {
        return meta.getType();
    }

    public void setStringValue(String value) {
        if (BITMASK.equals(getType())) {
            int newValue = Integer.valueOf(value);
            int currentValue = this.value.getAsInt();
            newValue = newValue | currentValue;
            value = String.valueOf(newValue);
        }
        if (onSetValue != null) {
            onSetValue.operation(value);
        }
    }

    public void setPath(List<String> currentPath) { }

    public List<String> getPath() { return path; }
}
