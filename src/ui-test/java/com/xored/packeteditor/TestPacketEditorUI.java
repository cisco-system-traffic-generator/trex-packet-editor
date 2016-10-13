package com.xored.packeteditor;

import javafx.scene.control.Label;
import org.junit.Test;

import static javafx.scene.input.KeyCode.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.util.NodeQueryUtils.hasText;

public class TestPacketEditorUI extends TestPacketEditorUIBase {

    @Test
    public void should_create_new_document() {
        newDocument();
    }

    @Test
    public void should_build_tcpip_stack() {
        addLayer("Internet Protocol Version 4");
        verifyThat("#Ether-IP-version", hasText("4"));
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("IPv4"));
        selectProtoType("IPv4");
        push(ENTER); // TODO: remove me once extra enter is not required
        verifyThat("#Ether-type", (Label t) -> t.getStyleClass().contains("field-value-set"));
    }

    @Test
    public void should_set_enum_value_as_text() {
        setComboFieldText("#Ether-type", "0x800"); // IPv4
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("IPv4"));
        verifyThat("#Ether-type", this::fieldLabelIsSet);

        setComboFieldText("#Ether-type", ""); // clear and set default
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("LOOP")); // default value for Ether
        verifyThat("#Ether-type", (Label t) -> !fieldLabelIsSet(t));

        setComboFieldText("#Ether-type", "0x86DD"); // IPv6
        verifyThat("#Ether-type", (Label t) -> t.getText().contains("IPv6"));
        verifyThat("#Ether-type", this::fieldLabelIsSet);
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
        newDocument();
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
