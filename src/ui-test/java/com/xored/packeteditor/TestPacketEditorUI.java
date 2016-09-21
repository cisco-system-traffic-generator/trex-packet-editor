package com.xored.packeteditor;

import com.xored.javafx.packeteditor.JavaFXBinaryPacketEditor;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import static javafx.scene.input.KeyCode.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.*;

public class TestPacketEditorUI extends ApplicationTest {
    @Override
    public void start(Stage stage) {
        try {
            new JavaFXBinaryPacketEditor().start(stage);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    public void should_have_load_pcap_button() {
        clickOn("Old");
        verifyThat("#fieldEditorBox", hasChild("#savepcapBtn"));
        clickOn("#loadpcapBtn");
        press(ESCAPE);
    }

    @Test
    public void should_have_save_pcap_button() {
        clickOn("Old");
        verifyThat("#fieldEditorBox", hasChild("Save pcap"));
        clickOn("Save pcap");
        press(ESCAPE);
    }

    public void addLayer(String layerType) {
        clickOn("Action");
        clickOn("Add Protocol");
        clickOn((ComboBox e)->true);
        clickOn(layerType);
        clickOn("OK");
    }

    @Test
    public void should_build_tcpip_stack() {
        addLayer("Ethernet II");
        addLayer("Internet Protocol Version 4");
        addLayer("TCP");
        clickOn("Action");
        clickOn("Build Packet");
    }

    @Test
    public void should_create_proto_on_enter() {
        clickOn("Action");
        clickOn("Add Protocol");
        press(ENTER);
    }

}
