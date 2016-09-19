package com.xored.javafx.packeteditor.data;

public class Field {
    public enum Type {
        BINARY, MAC_ADDRESS, IP_ADDRESS
    }

    private final String name;
    private final int offset;
    private final int globalOffset;
    private final int length;
    private final Type type;

    public Field(String name, int offset, int length, int globalOffset) {
        this.name = name;
        this.offset = offset;
        this.globalOffset = globalOffset;
        this.length = length;
        type = Type.BINARY;
    }

    public Field(String name, int offset, int length, Type type) {
        this.name = name;
        this.offset = offset;
        this.globalOffset = 0;
        this.length = length;
        this.type = type;
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

    public Type getType() {
        return type;
    }
}
