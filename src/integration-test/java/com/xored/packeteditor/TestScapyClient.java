package com.xored.packeteditor;

import com.google.gson.*;
import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import org.junit.*;
import org.junit.rules.Timeout;
import java.util.stream.Collectors;


import static com.xored.javafx.packeteditor.scapy.ScapyUtils.tcpIpTemplate;
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

    @Test
    public void buildPkt() {
        ScapyPkt pkt = scapy.build_pkt(tcpIpTemplate());
        JsonArray protos = pkt.getProtocols();
        assertEquals(((JsonObject)protos.get(0)).get("id").getAsString(), "Ether");
        assertEquals(((JsonObject)protos.get(1)).get("id").getAsString(), "IP");
        assertEquals(((JsonObject)protos.get(2)).get("id").getAsString(), "TCP");
    }

    @Test
    public void getTree() {
        JsonElement res = scapy.get_tree();
        assertNotNull(res);
    }
}
