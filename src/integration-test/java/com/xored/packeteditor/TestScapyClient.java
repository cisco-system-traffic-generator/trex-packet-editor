package com.xored.packeteditor;

import com.google.common.collect.Iterables;
import com.google.gson.*;
import com.xored.javafx.packeteditor.scapy.*;
import java.util.Arrays;
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

        FieldData sport = Iterables.find(pd.data.get(2).fields, field -> field.id.equals("sport"));

        assertEquals(sport.hvalue, "888");
        assertEquals(sport.getIntValue(), 888);
    }

    @Test
    public void getTree() {
        JsonElement res = scapy.get_tree();
        assertNotNull(res);
    }

    @Test
    public void rebuildPkt() {
        byte[] ethernet_pkt = scapy.build_pkt(Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.setValue("src", "11:22:33:44:55:66"),
                        ReconstructField.setValue("dst", "de:ad:be:ef:de:ad")
                ))
        )).getPacketBytes();

        PacketData new_pkt = scapy.reconstruct_pkt(ethernet_pkt, Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.randomizeValue("src"),
                        ReconstructField.setValue("dst", "aa:bb:cc:dd:ee:ff")
                ))
        ));

        ProtocolData ether = new_pkt.data.get(0);
        String newEtherDst = Iterables.find(ether.fields, f->f.id.equals("dst")).getStringValue();
        String newEtherSrc = Iterables.find(ether.fields, f->f.id.equals("src")).getStringValue();
        assertEquals(newEtherDst, "aa:bb:cc:dd:ee:ff");
        assertNotEquals(newEtherSrc, "11:22:33:44:55:66"); // src was randomized. chance to get same value is almost 0
    }
}
