package com.xored.javafx.packeteditor.scapy;

import com.google.gson.*;
import com.xored.javafx.packeteditor.data.JPacket;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;


public class ScapyServerClient {
    static Logger log = LoggerFactory.getLogger(ScapyServerClient.class);

    final Base64.Encoder base64Encoder = Base64.getEncoder();
    final Base64.Decoder base64Decoder = Base64.getDecoder();
    final Gson gson = new Gson();

    ZMQ.Context context;
    ZMQ.Socket session;
    String version_handler;
    int last_id = 0;

    static class Request {
        final String jsonrpc = "2.0";
        String id;
        String method;
        JsonElement params;
    }

    static class Response {
        String jsonrpc;
        String id;
        JsonElement result;
        JsonObject error;
    }

    public void open(String url) {
        close();
        context = ZMQ.context(1);
        session = context.socket(ZMQ.REQ);
        session.connect(url);

        JsonElement result = request("get_version", null);
        if (result == null) {
            log.error("get_version returned null");
            throw new ScapyException("Failed to get Scapy version");
        }
        // version: "1.01"
        log.info("Scapy version is {}", result.getAsJsonObject().get("version").getAsString());

        JsonArray vers = new JsonArray();
        vers.add("1");
        vers.add("01");
        result = request("get_version_handler", vers);
        if (result == null) {
            log.error("get_version returned null");
            throw new ScapyException("Failed to get version handler");
        }
        version_handler = result.getAsString();
    }

    public void close() {
        if (session != null) {
            session.close();
            session = null;
        }

        if (context != null) {
            context.term();
            context = null;
        }
    }

    /** makes request to Scapy server, returns Scapy server result */
    public JsonElement request(String method, JsonElement params) {
        Request reqs = new Request();
        reqs.id = Integer.toString(++last_id);
        reqs.method = method;
        reqs.params = params;

        String request_json = gson.toJson(reqs);
        log.debug(" sending: {}", request_json);

        session.send(request_json.getBytes(), 0);

        byte[] response_bytes = session.recv(0);
        if (response_bytes == null) {
            log.info("Received null response");
            throw new NullPointerException();
        }

        String response_json = new String(response_bytes, java.nio.charset.StandardCharsets.UTF_8);
        log.debug("received: {}", response_json);

        Response resp = gson.fromJson(response_json, Response.class);

        if (!resp.id.equals(reqs.id)) {
            log.error("received id:{}, expected:{}", resp.id, reqs.id);
            throw new ScapyException("unexpected result id");
        }

        if (resp.error != null) {
            String error_msg = resp.error.get("message").getAsString();
            log.error("received error: {}", error_msg);
            throw new ScapyException(error_msg);
        }

        return resp.result;
    }

    /** builds packet from JSON definition using scapy */
    public ScapyPkt build_pkt(JsonElement params) {
        JsonArray payload = new JsonArray();
        payload.add(version_handler);
        payload.add(params);
        ScapyPkt pkt = new ScapyPkt(request("build_pkt", payload));
        return pkt;
    }

    /** reads first packet from binary pcap file */
    public ScapyPkt read_pcap_packet(byte[] pcap_binary) {
        JsonArray payload = new JsonArray();
        String payload_b64 = base64Encoder.encodeToString(pcap_binary);
        payload.add(version_handler);
        payload.add(payload_b64);
        JsonArray pcap_packets = (JsonArray)request("read_pcap", payload);
        ScapyPkt pkt = new ScapyPkt(pcap_packets.get(0)); // get only 1st packet, ignore others
        return pkt;
    }

    /** write single pcap packet to a file, returns result binary pcap file content */
    public byte[] write_pcap_packet(byte[] packet_binary) {
        JsonArray packets = new JsonArray();
        packets.add(base64Encoder.encodeToString(packet_binary));

        JsonArray payload = new JsonArray();
        payload.add(version_handler);
        payload.add(packets);
        String pcap_base64 = request("write_pcap", payload).getAsString();
        return base64Decoder.decode(pcap_base64);
    }

    public JsonElement get_tree() {
        JsonArray payload = new JsonArray();
        payload.add(version_handler);
        return request("get_tree", payload);
    }

    /** builds packet from bytes, modifies fields */
    public JsonObject reconstruct_pkt (byte[] bytes, JPacket modify) {
        JsonArray param = new JsonArray();
        param.add(version_handler);
        param.add(Base64.getEncoder().encodeToString(bytes));
        if (modify != null) {
            param.add(packetToJson(modify));
        } else {
            param.add(JsonNull.INSTANCE);
        }
        JsonElement result = request("reconstruct_pkt", param);
        return result.getAsJsonObject();
    }


    public JsonElement packetToJson (JPacket pack) {
        return gson.toJsonTree(pack);
    }


    public JPacket packetFromJson (JsonElement npack) {
        JsonArray na = npack.getAsJsonArray();
        List<JPacket.Proto> protos = new ArrayList<>(na.size());
        for (JsonElement np : na) {
            JsonObject nproto = np.getAsJsonObject();
            JPacket.Proto proto = new JPacket.Proto(nproto.getAsJsonPrimitive("id").getAsString());
            proto.offset = nproto.getAsJsonPrimitive("offset").getAsInt();

            for (JsonElement nf : nproto.getAsJsonArray("fields")) {
                JsonObject nfield = nf.getAsJsonObject();
                JsonPrimitive nvalue = nfield.getAsJsonPrimitive("value");
                Object value = nvalue.isString() ? nvalue.getAsString() : nvalue.getAsInt();

                JPacket.Field field = new JPacket.Field(nfield.getAsJsonPrimitive("id").getAsString(),
                                                        value);
                field.offset = nfield.getAsJsonPrimitive("offset").getAsInt();
                field.length = nfield.getAsJsonPrimitive("length").getAsInt();
                proto.fields.add(field);
            }

            protos.add(proto);
        }
        return new JPacket(protos);
    }
}

