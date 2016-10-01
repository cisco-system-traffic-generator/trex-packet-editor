package com.xored.packeteditor;

import com.xored.javafx.packeteditor.TRexPacketCraftingTool;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static javafx.scene.input.KeyCode.*;
import static org.junit.Assert.fail;

public class TestPacketEditorUI extends ApplicationTest {

    TRexPacketCraftingTool trex = new TRexPacketCraftingTool();
    FieldEditorController editorController = trex.getInjector().getInstance(FieldEditorController.class);
    URL resources;
    {
        try {
            resources = new URL(getClass().getResource("/"), "../../resources/test/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            trex.start(stage);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Runs the specified {@link Runnable} on the
     * JavaFX application thread and waits for completion.
     *
     * @param action the {@link Runnable} to run
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static void runAndWait(Runnable action) throws InterruptedException {
        if (action == null)
            throw new NullPointerException("action");

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        // queue on JavaFX thread and wait for completion
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                doneLatch.countDown();
            }
        });

        doneLatch.await();
    }

    @Test
    public void load_pcap_file() {
        Assert.assertNotNull(editorController);
        final String[] error = {null};
        try {
            runAndWait(() -> {
                try {
                    File file = new File(resources.getFile() + "http.pcap");
                    editorController.loadPcapFile(file, true);
                } catch (Exception e) {
                    error[0] = e.getMessage();
                }
            });
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        if (error[0] != null) {
            fail(error[0]);
        }
        sleep(2, TimeUnit.SECONDS);
    }

    @Test
    public void load_and_save_pcap_file() {
        Assert.assertNotNull(editorController);
        final String[] error = {null};
        try {
            runAndWait(() -> {
                try {
                    File file = new File(resources.getFile() + "http.pcap");
                    editorController.loadPcapFile(file, true);
                    File file2 = new File(resources.getFile() + "http-2.pcap");
                    editorController.writeToPcapFile(file2, true);
                } catch (Exception e) {
                    error[0] = e.getMessage();
                }
            });
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        if (error[0] != null) {
            fail(error[0]);
        }
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

    private void addLayer(String layerType) {
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

        // TODO: do we need to fix the following problem at app level?
        // The 'LOOP' is selected in combobox, so we need to select IPv4
        clickOn((ComboBox e)->true);
        clickOn("IPv4");

        clickOn("Action");
        clickOn("Recalculate auto-values");
    }

    @Test
    public void should_create_proto_on_enter() {
        clickOn("Action");
        clickOn("Add Protocol");
        push(ENTER);
    }

}
