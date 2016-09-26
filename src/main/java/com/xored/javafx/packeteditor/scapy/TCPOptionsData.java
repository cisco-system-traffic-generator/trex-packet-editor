package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class TCPOptionsData {
    public String operation;

    private static Logger logger = LoggerFactory.getLogger(TCPOptionsData.class);

    public JsonElement value;

    public String getName() { return operation; }
    public String getDisplayValue() { return value != null ? value.toString() : ""; }

    public boolean hasValue() { return value != null && !value.isJsonNull(); }

    TCPOptionsData(String operation, JsonElement value) {
        this.operation = operation;
        this.value = value;
    }

    public static List<TCPOptionsData> fromValue(JsonElement value) {
        List<TCPOptionsData> options = new LinkedList<>();
        try {
            if (value instanceof JsonArray) {
                for (JsonElement operation_tuple: value.getAsJsonArray()) {
                    options.add(new TCPOptionsData(
                            operation_tuple.getAsJsonArray().get(0).getAsString(),
                            operation_tuple.getAsJsonArray().get(1)
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Unable to parse value: {} due to: {}", value, e);
        }
        return options;
    }

    // python representation
    //[('MSS', 1460), ('NOP', None), ('NOP', None), ('SAckOK', '')]
    // JSON representation:
    //'[["MSS", 1460], ["NOP", null], ["NOP", null], ["SAckOK", ""]]'
}
