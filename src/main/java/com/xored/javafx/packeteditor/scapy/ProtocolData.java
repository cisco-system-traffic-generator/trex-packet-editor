package com.xored.javafx.packeteditor.scapy;

import java.util.List;

/**
 * Holds a data for particular protocol
 * build_pkg/reconstruct_pkg produces this
 */
public class ProtocolData {
    public String id;
    public List<FieldData> fields;
}
