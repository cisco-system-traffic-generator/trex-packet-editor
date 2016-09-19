package com.xored.javafx.packeteditor.data;

interface IField {
    enum Type {
        BINARY, MAC_ADDRESS, IP_ADDRESS, STRING, PROTOCOL, NONE
    }

    int getOffset();
    int getAbsOffset();
    int getLength();
    String getName();
    String getDisplayValue();
    Type getType();
}

