package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class FieldData {
    public String id;

    /** Scapy value. can be primitive(string, number) or custom object(array/dict). can be serialized and passed back build_pkt */
    public JsonElement value;

    /** human-readable representation of the object. */
    public String hvalue;

    public Number offset;
    public Number length;

    /** true, if this field was used for binary representation generation. no length/offset if not */
    public Boolean ignored; // optional

    public String getId() {
        return id;
    }

    public String getStringValue() { return value.getAsString(); }
    public int getIntValue() { return value.getAsInt(); }

    public String getHumanValue() { return hvalue; }

    /** check if field has length and offset specified */
    public boolean hasPosition() { return length != null && offset != null; }

    public int getLength() { return length.intValue(); }
    public int getOffset() { return offset.intValue(); }

    /** this field is ignored for current protocol configuration */
    public boolean isIgnored() { return ignored != null && ignored; }

    public FieldValue.ObjectType getObjectValueType() { return FieldValue.getObjectValueType(value); }
    public boolean isPrimitive() { return value instanceof JsonPrimitive; }
    public boolean isObject() { return getObjectValueType() != null; }

    /** returns value. can be primitive(number/string) or a custom object(bytes, expressions, ...). see ObjectType */
    public JsonElement getValue() { return value; }

    /** returns bytes if this is a byte[] field or null otherwise */
    public byte[] getBytes() { return FieldValue.getBytes(value); }

    /** returns scapy value expression or null */
    public String getValueExpr() {
        return FieldValue.getExpression(value);
    }
}
