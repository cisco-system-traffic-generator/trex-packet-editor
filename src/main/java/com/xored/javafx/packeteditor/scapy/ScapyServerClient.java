package com.xored.javafx.packeteditor.scapy;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/** binding to scapy server */
public class ScapyServerClient {
    public static final int ZMQ_THREADS = 1;
    static Logger logger = LoggerFactory.getLogger(ScapyServerClient.class);

    final Base64.Encoder base64Encoder = Base64.getEncoder();
    final Base64.Decoder base64Decoder = Base64.getDecoder();
    final Gson gson = new Gson();

    ZMQ.Context zmqContext;
    ZMQ.Socket zmqSocket;
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
        zmqContext = ZMQ.context(ZMQ_THREADS);
        zmqSocket = zmqContext.socket(ZMQ.REQ);
        logger.info("connecting to scapy_server at {}", url);
        zmqSocket.connect(url);

        version_handler = getVersionHandler();
    }

    private JsonArray getVersion() {
        JsonElement result = request("get_version", null);
        if (result == null) {
            logger.error("get_version returned null");
            throw new ScapyException("Failed to get Scapy version");
        }

        String versionString = result.getAsJsonObject().get("version").getAsString();
        logger.info("Scapy version is {}", versionString);

        JsonArray version = new JsonArray();

        Arrays.stream(versionString.split("\\."))
              .forEach(version::add);
        return version;
    }
    
    private String getVersionHandler() {
        JsonElement versionHandler = request("get_version_handler", getVersion());
        if (versionHandler == null) {
            logger.error("get_version returned null");
            throw new ScapyException("Failed to get version handler");
        }
        return versionHandler.getAsString();
    }
    
    public void close() {
        if (zmqSocket != null) {
            zmqSocket.close();
            zmqSocket = null;
        }

        if (zmqContext != null) {
            zmqContext.term();
            zmqContext = null;
        }
    }

    /** makes request to Scapy server, returns Scapy server result */
    public JsonElement request(String method, JsonElement params) {
        Request reqs = new Request();
        reqs.id = Integer.toString(++last_id);
        reqs.method = method;
        reqs.params = params;

        String request_json = gson.toJson(reqs);
        logger.debug(" sending: {}", request_json);

        zmqSocket.send(request_json.getBytes(), 0);

        byte[] response_bytes = zmqSocket.recv(0);
        if (response_bytes == null) {
            logger.info("Received null response");
            throw new NullPointerException();
        }

        String response_json = new String(response_bytes, java.nio.charset.StandardCharsets.UTF_8);
        logger.debug("received: {}", response_json);

        Response resp = gson.fromJson(response_json, Response.class);

        if (!resp.id.equals(reqs.id)) {
            logger.error("received id:{}, expected:{}", resp.id, reqs.id);
            throw new ScapyException("unexpected result id");
        }

        if (resp.error != null) {
            String error_msg = resp.error.get("message").getAsString();
            logger.error("received error: {}", error_msg);
            throw new ScapyException(error_msg);
        }

        return resp.result;
    }

    /** builds packet from JSON definition using scapy */
    public PacketData build_pkt(JsonElement params) {
        JsonArray payload = new JsonArray();
        payload.add(version_handler);
        payload.add(params);
        return packetFromJson(request("build_pkt", payload));
    }

    public PacketData build_pkt(List<ReconstructProtocol> protocols) {
        return build_pkt(gson.toJsonTree(protocols));
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
    public PacketData read_pcap_packet(byte[] pcap_binary) {
        JsonArray payload = new JsonArray();
        String payload_b64 = base64Encoder.encodeToString(pcap_binary);
        payload.add(version_handler);
        payload.add(payload_b64);
        JsonArray pcap_packets = (JsonArray)request("read_pcap", payload);
        JsonElement first_packet = pcap_packets.get(0);
        return packetFromJson(first_packet);
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

    /** builds packet from bytes */
    public PacketData reconstruct_pkt(byte[] packet_binary) {
        JsonObject result = reconstruct_pkt(packet_binary, new JsonArray());
        return gson.fromJson(result, PacketData.class);
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

    private PacketData packetFromJson(JsonElement packet) {
        return gson.fromJson(packet, PacketData.class);
    }
}

