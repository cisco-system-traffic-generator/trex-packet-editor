package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Base64;

public class FieldData {
    public String id;
    public String field_type; // name of the Scapy class, optional metadata
    public JsonElement value;
    public String hvalue;
    public String value_base64; // optional
    public Number offset;
    public Number length;
    public Boolean ignored; // optional

    public String getId() {
        return id;
    }

    public String getStringValue() { return value.getAsString(); }
    public int getIntValue() { return value.getAsInt(); }

    public int getLength() { return length.intValue(); }
    public int getOffset() { return offset.intValue(); }

    /** this field is ignored for current protocol configuration */
    public boolean isIgnored() { return ignored != null && ignored; }

    /** if value can not be passed as JsonElement, it is passed as a value_base64 */
    public boolean hasValue() { return value != null && !value.isJsonNull(); }
    public JsonElement getValue() { return value; }
    public boolean hasBinaryData() { return value_base64 != null; }
    public byte[] getBinaryData() { return hasBinaryData() ? Base64.getDecoder().decode(value_base64) : null; }

    /** returns scapy value expression or null */
    public String getValueExpr() {
        if (value instanceof JsonObject) {
            if (value.getAsJsonObject().get("vtype") instanceof JsonPrimitive) {
                return value.getAsJsonObject().get("expr").getAsString();
            }
        }
        return null;
    }
}
