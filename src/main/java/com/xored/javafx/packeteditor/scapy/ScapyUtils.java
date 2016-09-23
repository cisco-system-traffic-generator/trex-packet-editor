package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ScapyUtils {
    public static JsonObject layer(String type) {
        JsonObject res = new JsonObject();
        res.add("id", new JsonPrimitive(type));
        return res;
    }

    // Ether()/IP()/TCP()
    public static JsonArray tcpIpTemplate() {
        JsonArray payload = new JsonArray();
        payload.add(layer("Ether"));
        payload.add(layer("IP"));
        payload.add(layer("TCP"));
        return payload;
    }

    /** generates payload for reconstruct_pkt
     * @param fieldPath: example: Ether, IP, TCP
     * @param fieldName: src
     * @param newValue: 127.0.0.1
     * @return 'modify' payload for reconstruct_pkt
     * */
    public static JsonArray createReconstructPktPayload(List<String> fieldPath, String fieldName, String newValue) {
        JsonObject innerProtocol = null;
        JsonArray protocols = new JsonArray();
        for (String protoId: fieldPath) {
            innerProtocol = new JsonObject();
            protocols.add(innerProtocol);
            innerProtocol.add("id", new JsonPrimitive(protoId));
        }
        JsonArray fieldsToModify = new JsonArray();
        innerProtocol.add("fields", fieldsToModify);
        JsonObject modifyFieldRecord = new JsonObject();
        fieldsToModify.add(modifyFieldRecord);
        modifyFieldRecord.add("id", new JsonPrimitive(fieldName));
        modifyFieldRecord.add("hvalue", new JsonPrimitive(newValue));
        return protocols;
    }

}

