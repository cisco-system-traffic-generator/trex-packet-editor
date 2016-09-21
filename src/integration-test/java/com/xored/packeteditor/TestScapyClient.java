package com.xored.packeteditor;

import com.google.gson.*;
import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import com.xored.javafx.packeteditor.data.JPacket;
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

    @Test
    public void rebuildPkt() {
        JsonArray pros = new JsonArray();
        JsonObject p = new JsonObject();
        p.addProperty("id", "Ether");
        pros.add(p);
        ScapyPkt spkt0 = scapy.build_pkt(pros);
        byte[] buffer0 = spkt0.getBinaryData();
        JPacket pack0 = scapy.packetFromJson(spkt0.getProtocols());

        JPacket.Proto proto0 = new JPacket.Proto("Ether");
        String src0 = "00:11:22:33:44:55";
        proto0.fields.add(new JPacket.Field("src", src0));
        JPacket model = new JPacket(Arrays.asList(proto0));
        pack0.get(0).getField("src").value = src0;
        
        JsonElement res1 = scapy.reconstruct_pkt(buffer0, model);
        ScapyPkt spkt1 = new ScapyPkt(res1);
        JPacket pack1 = scapy.packetFromJson(spkt1.getProtocols());
        assertTrue(equals(pack1, pack0));
    }
    
    
    static boolean equals (JPacket packet1, JPacket packet2) {
        
        if (packet1.size() != packet2.size())
            return false;
        
        for (int i = 0; i < packet1.size(); ++i) {
            JPacket.Proto proto1 = packet1.get(i),
                          proto2 = packet2.get(i);
            
            if (!proto1.id.equals(proto2.id)
             || proto1.offset != proto2.offset
             || proto1.fields.size() != proto2.fields.size())
                return false;
            
            for (int k = 0; k < proto1.fields.size(); ++k) {
                JPacket.Field field1 = proto1.fields.get(k),
                              field2 = proto2.fields.get(k);
                if (!field1.id.equals(field2.id)
                 || field1.offset != field2.offset
                 || field1.length != field2.length
                 || !field1.value.equals(field2.value))
                    return false;
            }
        }
        
        return true;
    }
}
