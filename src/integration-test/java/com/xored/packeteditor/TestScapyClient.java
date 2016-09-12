package com.xored.packeteditor;

import com.xored.javafx.packeteditor.remote.ScapyServerClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.junit.Assert.*;

public class TestScapyClient {
    final String server_url = System.getenv("SCAPY_SERVER") != null
            ? ("tcp://" + System.getenv("SCAPY_SERVER"))
            : "tcp://localhost:4507";

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Test
    public void connectTest() {
        ScapyServerClient scapy = new ScapyServerClient();
        boolean connected = scapy.open(server_url);
        assertTrue(connected);
    }
}
