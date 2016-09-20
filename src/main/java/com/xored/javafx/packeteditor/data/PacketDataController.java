package com.xored.javafx.packeteditor.data;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Observable;

public class PacketDataController extends Observable {
    static Logger log = LoggerFactory.getLogger(PacketDataController.class);
    @Inject ScapyServerClient scapy;
    @Inject IBinaryData binary;

    ScapyPkt pkt;

    public void init() {
        scapy.open("tcp://localhost:4507");
        ClassLoader classLoader = getClass().getClassLoader();
        File example_file = new File(classLoader.getResource("http_get_request.pcap").getFile());
        loadPcapFile(example_file);
    }

    public void read_pkt(ScapyPkt payload) {
        pkt = payload;
        byte [] bytes = pkt.getBinaryData();
        setChanged();
        binary.setBytes(bytes);
        notifyObservers(null);
    }

    public JsonArray getProtocols() {
        return pkt.getProtocols();
    }

    public boolean loadPcapFile(String filename) {
        return loadPcapFile(new File(filename));
    }

    public boolean loadPcapFile(File file) {
        try {
            byte[] bytes = Files.toByteArray(file);
            read_pkt(scapy.read_pcap_packet(bytes));
            return true;
        } catch (Exception e) {
            log.error("Failed to load pcap - {}", e);
        }
        return false;
    }

    public boolean writeToPcapFile(File file) {
        try {
            byte[] pcap_bin = scapy.write_pcap_packet(pkt.getBinaryData());
            Files.write(pcap_bin, file);
            return true;
        } catch (Exception e) {
            log.error("Failed to load pcap - {}", e);
        }
        return false;
    }
}
