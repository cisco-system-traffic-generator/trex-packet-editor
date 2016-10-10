package com.xored.packeteditor;

import com.google.gson.*;
import com.xored.javafx.packeteditor.scapy.*;
import java.util.Arrays;
import java.util.List;

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
        scapy.open(server_url);
    }

    @After
    public void cleanup() {
        scapy.close();
    }

    @Test
    public void should_build_pkt_from_definition() {
        PacketData pd = scapy.build_pkt(Arrays.asList(
                ReconstructProtocol.pass("Ether"),
                ReconstructProtocol.pass("IP"),
                ReconstructProtocol.modify("TCP", Arrays.asList(ReconstructField.setValue("sport", 888)))
        ));
        assertNotNull(pd.binary);
        assertEquals(pd.data.get(0).id, "Ether");
        assertEquals(pd.data.get(1).id, "IP");
        assertEquals(pd.data.get(2).id, "TCP");

        FieldData sport = pd.data.get(2).getFieldById("sport");

        assertEquals(sport.hvalue, "888");
        assertEquals(sport.getIntValue(), 888);
    }


    @Test
    public void should_build_pkt_with_hvalue() {
        PacketData pd = scapy.build_pkt(Arrays.asList(
                ReconstructProtocol.pass("Ether"),
                ReconstructProtocol.modify("IP", Arrays.asList(ReconstructField.setHumanValue("len", "123")))
        ));
        assertNotNull(pd.binary);
        assertEquals(pd.data.get(1).id, "IP");
        assertEquals(pd.data.get(1).getFieldById("len").getIntValue(), 123);
    }

    @Test
    public void getTree() {
        JsonElement res = scapy.get_tree();
        assertNotNull(res);
    }

    @Test
    public void rebuildPkt() {
        PacketData ethernet_pkt = scapy.build_pkt(Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.setValue("dst", "de:ad:be:ef:de:ad")
                ))
        ));
        String origEtherSrc = ethernet_pkt.getProtocols().get(0).getFieldById("src").getStringValue();
        assertEquals(origEtherSrc.length(), 17);

        PacketData new_pkt = scapy.reconstruct_pkt(ethernet_pkt.getPacketBytes(), Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.randomizeValue("src"),
                        ReconstructField.setValue("dst", "aa:bb:cc:dd:ee:ff")
                ))
        ));

        ProtocolData ether = new_pkt.data.get(0);
        assertEquals(ether.getFieldById("dst").getStringValue(), "aa:bb:cc:dd:ee:ff");
        assertNotEquals(ether.getFieldById("src").getStringValue(), origEtherSrc); // src was randomized. chance to get same value is almost 0
    }

    @Test
    public void should_return_ether_payload_classes() {
        List<String> etherPayload = scapy.get_payload_classes("Ether");

        assertTrue(etherPayload.contains("IP"));
        assertTrue(etherPayload.contains("Dot1Q"));
        assertTrue(etherPayload.contains("Raw"));

        assertTrue(!etherPayload.contains("Ether"));
        assertTrue(!etherPayload.contains("TCO"));
    }
}
