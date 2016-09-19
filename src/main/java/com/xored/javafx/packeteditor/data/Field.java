package com.xored.javafx.packeteditor.data;

public class Field {
    public enum Type {
        BINARY, MAC_ADDRESS, IP_ADDRESS, STRING, PROTOCOL, NONE
    }

    private String name;
    private int offset;
    private int globalOffset;
    private int length;
    private Type type;
    private String value;

    public Field(String name, int offset, int length, int globalOffset, String value, Type type) {
        this.name = name;
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

    public String getName() {
        return name;
    }
    public String getDisplayValue() { return value; }

    public Type getType() {
        return type;
    }
}
