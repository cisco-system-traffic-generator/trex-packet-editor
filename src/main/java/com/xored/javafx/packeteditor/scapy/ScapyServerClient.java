package com.xored.javafx.packeteditor.scapy;

import com.google.gson.*;

import java.util.Arrays;
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
        log.info("connecting to scapy_server at {}", url);
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
    public JsonObject build_pkt(JsonElement params) {
        JsonArray payload = new JsonArray();
        payload.add(version_handler);
        payload.add(params);
        return request("build_pkt", payload).getAsJsonObject();
    }

    public PacketData build_pkt(List<ReconstructProtocol> protocols) {
        JsonObject result = build_pkt(gson.toJsonTree(protocols));
        return gson.fromJson(result, PacketData.class);
    }

    public ScapyDefinitions get_definitions() {
        JsonArray payload = new JsonArray();
        payload.add(version_handler);
        payload.add(JsonNull.INSTANCE);
        JsonElement res = request("get_definitions", payload);
        return gson.fromJson(res, ScapyDefinitions.class);
    }

    public List<String> get_payload_classes(List<ReconstructProtocol> protocols) {
        JsonArray payload = new JsonArray();
        payload.add(version_handler);
        payload.add(gson.toJsonTree(protocols));
        JsonElement res = request("get_payload_classes", payload);
        return Arrays.asList(gson.fromJson(res, String[].class));
    }

    public List<String> get_payload_classes(String protocolId) {
        return get_payload_classes(Arrays.asList(ReconstructProtocol.pass(protocolId)));
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
    public PacketData reconstruct_pkt(byte[] packet_binary, List<ReconstructProtocol> protocols) {
        JsonObject result = reconstruct_pkt(packet_binary, gson.toJsonTree(protocols));
        return gson.fromJson(result, PacketData.class);
    }

    /** builds packet from bytes, modifies fields */
    public JsonObject reconstruct_pkt (byte[] packet_binary, JsonElement modify) {
        JsonArray param = new JsonArray();
        param.add(version_handler);
        param.add(Base64.getEncoder().encodeToString(packet_binary));
        param.add(modify);
        JsonElement result = request("reconstruct_pkt", param);
        return result.getAsJsonObject();
    }

}

