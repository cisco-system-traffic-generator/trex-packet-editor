package com.xored.javafx.packeteditor.scapy;

import java.util.List;

/**
 * Holds a data for particular protocol
 * build_pkg/reconstruct_pkg produces this
 */
public class ProtocolData {
    public String id; // classId
    public String real_id; // classId of a class, which will be shown after serialization
    public Boolean valid_structure; // False if scapy_server detected incorrect structure
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

    /** return classId obtained after deserialization from binary */
    public String getRealId() { return real_id; }

    /** scapy class has been changed/specialized */
    public boolean protocolRealIdDifferent() { return (real_id != null) && !real_id.equals(id); }

    /** true if scapy server detected wrong structure */
    public boolean isInvalidStructure() { return valid_structure != null && valid_structure == false; }

}
