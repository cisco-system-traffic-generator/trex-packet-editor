package com.xored.packeteditor;

import com.xored.javafx.packeteditor.JavaFXBinaryPacketEditor;
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
        verifyThat("#fieldEditorPane", hasChild("#loadpcapBtn"));
        verifyThat("#fieldEditorPane", hasChild("#savepcapBtn"));
        clickOn("#loadpcapBtn");
        press(ESCAPE);
    }

    @Test
    public void should_have_save_pcap_button() {
        clickOn("Old");
        verifyThat("#fieldEditorPane", hasChild("Save pcap"));
        clickOn("Save pcap");
        press(ESCAPE);
    }

}
