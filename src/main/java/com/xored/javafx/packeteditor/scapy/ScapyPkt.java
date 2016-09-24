package com.xored.javafx.packeteditor.scapy;

import com.google.gson.*;

import java.util.Base64;


// @deprecated. this class will be removed and
// PacketData will be used instead
public class ScapyPkt {
    private final JsonObject pkt;
    private final Base64.Decoder base64Decoder = Base64.getDecoder();
    private final Gson gson = new Gson();

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

    /** returns data as POJO */
    public PacketData packet() {
        return gson.fromJson(pkt, PacketData.class);
    }
}
