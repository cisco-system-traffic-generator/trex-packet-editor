package com.xored.packeteditor;

import com.google.gson.*;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import org.junit.*;
import org.junit.rules.Timeout;

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
    public void getVersion() {
        JsonElement res = scapy.build_pkt(tcpIpTemplate());
        assertNotNull(res);
    }

    @Test
    public void getTree() {
        JsonElement res = scapy.get_tree();
        assertNotNull(res);
    }
}
