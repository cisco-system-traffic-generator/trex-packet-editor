package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.metatdata.FieldMetadata;

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
        RAW,
        NUMBER,
        ENUM,
        BITMASK,
        IPV4OPTIONS,
        IPV4ADDRESS,
        IPOPTIONS,
        TCP_OPTIONS,
    }

    FieldMetadata getMeta();
    String getId();
    String getName();
    String getDisplayValue();
    Type getType();
    List<String> getPath();
}

