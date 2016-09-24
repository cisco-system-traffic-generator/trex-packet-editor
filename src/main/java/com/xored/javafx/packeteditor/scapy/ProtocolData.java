package com.xored.javafx.packeteditor.scapy;

import java.util.List;

/**
 * Holds a data for particular protocol
 * build_pkg/reconstruct_pkg produces this
 */
public class ProtocolData {
    public String id; // classId
    public String name; // protocol name
    public Number offset;
    public List<FieldData> fields;
}
