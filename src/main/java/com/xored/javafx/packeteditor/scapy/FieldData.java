package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;

public class FieldData {
    public String id;
    public JsonElement value;
    public String hvalue;
    public Number offset;
    public Number length;

    public String getStringValue() { return value.getAsString(); }
    public int getIntValue() { return value.getAsInt(); }

    public int getLength() { return length.intValue(); }
    public int getOffset() { return offset.intValue(); }
}
