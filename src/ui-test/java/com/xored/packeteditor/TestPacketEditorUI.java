package com.xored.packeteditor;

import org.junit.Test;
import static javafx.scene.input.KeyCode.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.hasText;

public class TestPacketEditorUI extends TestPacketEditorUIBase {

    @Test
    public void should_create_proto_on_enter() {
        clickOn("Action");
        clickOn("Add Protocol");
        push(ENTER);
    }

    @Test
    public void should_build_tcpip_stack() {
        addLayer("Ethernet II");
        selectProtoType("IPv4");
        addLayer("Internet Protocol Version 4");
        verifyThat("#Ether-IP-version", hasText("4"));
        recalculateAutoValues();
    }

    @Test
    public void should_have_load_pcap_button() {
        clickOn("File");
        clickOn("Load pcap file");
        push(ESCAPE);
    }

    @Test
    public void should_have_save_pcap_button() {
        clickOn("File");
        clickOn("Save to pcap file");
        push(ESCAPE);
    }

    @Test
    public void load_pcap_file() {
        loadPcapFile("http.pcap");
        verifyThat("#Ether-IP-version", hasText("4"));
        verifyThat("#Ether-IP-TCP-seq", hasText("951057939"));
    }

    @Test
    public void load_pcap_file_neg() {
        loadPcapFileEx("http.bad-pcap", true);
    }

    @Test
    public void load_and_save_pcap_file() {
        loadPcapFile("http.pcap");
        savePcapFile("http-2.pcap");
        clickOn("File");
        clickOn("New Document");
        loadPcapFile("http-2.pcap");
        verifyThat("#Ether-IP-version", hasText("4"));
        verifyThat("#Ether-IP-TCP-seq", hasText("951057939"));
    }

    @Test
    public void load_and_save_pcap_file_neg() {
        loadPcapFile("http.pcap");
        savePcapFileEx("/http-2.pcap", true);
    }

}
