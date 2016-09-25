package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.FieldMetadata;

import java.util.List;

public interface IField {

    String DEFAULT = "default";
    String RANDOM = "random";

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

    FieldMetadata getMeta();
    int getOffset();
    int getAbsOffset();
    int getLength();
    String getId();
    String getName();
    String getDisplayValue();
    Type getType();
    void setStringValue(String value);
    List<String> getPath();
}

