package com.xored.javafx.packeteditor.data;

public interface IField {
    // TODO: move to metadata
    enum Type {
        BINARY,
        MAC_ADDRESS,
        IP_ADDRESS,
        STRING,
        PROTOCOL,
        NONE,
        NUMBER,
        ENUM,
        BITMASK,
        IPV4OPTIONS,
        IPV4ADDRESS,
        IPOPTIONS
    }

    int getOffset();
    int getAbsOffset();
    int getLength();
    String getName();
    String getDisplayValue();
    Type getType();
}

