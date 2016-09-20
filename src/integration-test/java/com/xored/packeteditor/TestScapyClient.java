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

        JPacket.Proto proto = new JPacket.Proto("Ether");
        String src0 = "00:11:22:33:44:55";
        proto.fields.add(new JPacket.Field("src", src0));
        JPacket pack = new JPacket(Arrays.asList(proto));
        
        JsonElement res1 = scapy.reconstruct_pkt(buffer0, pack);
        ScapyPkt spkt1 = new ScapyPkt(res1);
        
        JsonArray layers1 = spkt1.getProtocols();
        assertTrue(layers1.size() == 1);
        JsonObject layer1 = layers1.get(0).getAsJsonObject();
        assertTrue(layer1.has("fields"));
        JsonArray fields1 = layer1.getAsJsonArray("fields");
        for (JsonElement field1 : fields1)
        {
            JsonObject obj1 = field1.getAsJsonObject();
            if (obj1.getAsJsonPrimitive("id").getAsString().equals("src"))
            {
                String src1 = obj1.getAsJsonPrimitive("value").getAsString();
                assertEquals(src1, src0);
                return;
            }
        }
        
        assertTrue(false);
    }
}
