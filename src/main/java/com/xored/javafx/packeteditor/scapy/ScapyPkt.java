package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Base64;
import java.util.stream.Collectors;

public class ScapyPkt {
    private final JsonObject pkt;
    private Base64.Decoder base64Decoder = Base64.getDecoder();
    
    public ScapyPkt(JsonElement response) {
        this.pkt = (JsonObject) response;
    }
    
    public byte[] getBinaryData() {
        String binary = pkt.get("binary").getAsString();
        return base64Decoder.decode(binary);
    }
    
    public JsonArray getProtocols() {
        return (JsonArray)pkt.get("data");
    }
}
