package com.xored.packeteditor;

import com.google.gson.*;
import com.xored.javafx.packeteditor.remote.ScapyServerClient;
import org.junit.*;
import org.junit.rules.Timeout;

import static org.junit.Assert.*;

public class TestScapyClient {
    final String server_url = System.getenv("SCAPY_SERVER") != null
            ? ("tcp://" + System.getenv("SCAPY_SERVER"))
            : "tcp://localhost:4507";

    ScapyServerClient scapy;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);


    @Before
    public void init() {
        scapy = new ScapyServerClient();
        boolean connected = scapy.open(server_url);
        assertTrue(connected);
    }

    @After
    public void cleanup() {
        scapy.close();
    }

    JsonObject layer(String type) {
        JsonObject res = new JsonObject();
        res.add("id", new JsonPrimitive(type));
        return res;
    }

    @Test
    public void getVersion() {
        JsonArray payload = new JsonArray();
        payload.add(layer("Ether"));
        payload.add(layer("TCP"));
        payload.add(layer("IP"));

        JsonElement res = scapy.build_pkt(payload);
        assertNotNull(res);
    }
}
