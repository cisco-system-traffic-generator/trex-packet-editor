package com.xored.javafx.packeteditor.data;

import java.util.List;

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
    String getId();
    String getName();
    String getDisplayValue();
    Type getType();
    void setStringValue(String value);
    void setPath(List<String> currentPath);
    List<String> getPath();
}

