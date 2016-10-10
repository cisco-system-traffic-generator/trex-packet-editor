package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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

    public static List<TCPOptionsData> fromFieldData(FieldData value) {
        List<TCPOptionsData> options = new LinkedList<>();
        try {
            String expr = value.getValueExpr();
            if (expr == null) {
                return options;
            }

            // convert python string expression to JSON-like object for quick parsing
            // TODO: use stringbuilder
            String jsonLikeExpr = expr.replace("b'", "'").replace("'", "\"").replace('(', '[').replace(')', ']');

            JsonElement payload = new JsonParser().parse(jsonLikeExpr);
            if (!payload.isJsonArray()) {
                // scapy can return {}, which is not supported
                return options;
            }
            JsonArray optionsArray = payload.getAsJsonArray();
            for (JsonElement option: optionsArray) {
                JsonArray opt_tuple = option.getAsJsonArray();
                options.add(new TCPOptionsData(
                        opt_tuple.get(0).getAsString(),
                        opt_tuple.get(1)));
            }
        } catch (Exception e) {
            logger.error("Unable to parse value: {} due to: {}", value, e);
        }
        return options;
    }

    // python representation
    //[('MSS', 1460), ('NOP', None), ('NOP', None), ('SAckOK', '')]
    // python3 representation
    //[('MSS', 1460), ('NOP', None), ('NOP', None), ('SAckOK', b'')]
    // JSON representation:
    //'[["MSS", 1460], ["NOP", null], ["NOP", null], ["SAckOK", ""]]'
}
