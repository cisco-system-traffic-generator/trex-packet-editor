package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;

/**
 * generates structure for reconstruct_packet
 */
public class ReconstructPacketBuilder {
    JsonArray protocols;

    public ReconstructPacketBuilder() {
        protocols = new JsonArray();
    }

    /** receives packet data and removes all fields, except protocols with "id"
     * [{"id": "Ether", ...}, {"id": "IP", ...}]
     * [{"id": "Ether"}, {"id": "IP"}]
     * */
    public void appendStructureFromPacket(JsonArray protocolDef) {
        for (JsonElement protocol: protocolDef) {
            String protocolId = protocol.getAsJsonObject().get("id").getAsString();
            appendProtocol(protocolId);
        }
    }

    public void appendStructureFromPath(List<String> protocolPath) {
        for (String protocolId: protocolPath) {
            appendProtocol(protocolId);
        }
    }

    public void appendProtocol(String protocolId) {
        JsonObject protocolDef = new JsonObject();
        protocolDef.add("id", new JsonPrimitive(protocolId));
        protocols.add(protocolDef);
    }

    /** remove inner protocol record with Scapy */
    public void markProtocolForDeletion() {
        JsonObject protocolDef = protocols.get(protocols.size() - 1).getAsJsonObject();
        protocolDef.add("delete", new JsonPrimitive(true));
    }

    public JsonArray getProtocols() {
        return protocols;
    }

}

