package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Base64;


public class ScapyPkt {
    private final JsonObject pkt;
    private final Base64.Decoder base64Decoder = Base64.getDecoder();

    public ScapyPkt() {
        pkt = new JsonObject();
        pkt.add("binary", new JsonPrimitive(""));
        pkt.add("data", new JsonArray());
    }

    public ScapyPkt(JsonElement response) {
        pkt = (JsonObject) response;
    }
    
    public byte[] getBinaryData() {
        String binary = pkt.get("binary").getAsString();
        return base64Decoder.decode(binary);
    }
    
    public JsonArray getProtocols() {
        return (JsonArray)pkt.get("data");
    }
}
