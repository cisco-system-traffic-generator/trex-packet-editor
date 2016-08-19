package com.xored.javafx.packeteditor.data;

public class Field {
    private final int offset;
    private final int length;

    public Field(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }
}
