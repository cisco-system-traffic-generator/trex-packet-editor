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

    /** returns field by Id or null */
    public FieldData getFieldById(String fieldId) {
        return fields.stream().filter(f->f.getId().equals(fieldId)).findFirst().orElse(null);
    }

    public String getId() {
        return id;
    }

    public List<FieldData> getFields() {
        return fields;
    }

}
