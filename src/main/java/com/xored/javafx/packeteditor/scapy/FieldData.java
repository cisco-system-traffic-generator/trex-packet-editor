package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;

import java.util.Base64;

public class FieldData {
    public String id;
    public JsonElement value;
    public String hvalue;
    public String value_base64;
    public Number offset;
    public Number length;
    public Boolean ignored;

    public String getStringValue() { return value.getAsString(); }
    public int getIntValue() { return value.getAsInt(); }

    public int getLength() { return length.intValue(); }
    public int getOffset() { return offset.intValue(); }

    /** this field is ignored for current protocol configuration */
    public boolean isIgnored() { return ignored != null && ignored.booleanValue(); }

    /** if value can not be passed as JsonElement, it is passed as a value_base64 */
    public boolean hasBinaryData() { return value_base64 != null; }
    public byte[] getBinaryData() { return hasBinaryData() ? Base64.getDecoder().decode(value_base64) : null; }
}
