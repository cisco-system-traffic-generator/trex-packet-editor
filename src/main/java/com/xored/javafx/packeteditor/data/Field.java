package com.xored.javafx.packeteditor.data;

public class Field {
    private final String name;
    private final int offset;
    private final int length;

    public Field(String name, int offset, int length) {
        this.name = name;
        this.offset = offset;
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getName() {
        return name;
    }
}
