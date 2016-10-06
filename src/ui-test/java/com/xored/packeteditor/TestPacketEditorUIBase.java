package com.xored.packeteditor;

import com.xored.javafx.packeteditor.TRexPacketCraftingTool;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import javafx.application.Platform;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.junit.Assert;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.service.query.NodeQuery;

import static javafx.scene.input.KeyCode.CONTROL;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.UP;
import static org.junit.Assert.fail;

/**
 * Base class for UI tests. supports headless testing
 */
public class TestPacketEditorUIBase extends ApplicationTest {
    static org.slf4j.Logger logger = LoggerFactory.getLogger(TRexPacketCraftingTool.class);

    static boolean isHeadless() { return Boolean.getBoolean("headless"); }

    static {
        if (isHeadless()) {
            logger.info("using headless ui tests");
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
        }
    }

    /**
     * Runs the specified {@link Runnable} on the
     * JavaFX application thread and waits for completion.
     *
     * @param action the {@link Runnable} to run
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static void runAndWait(Runnable action) throws InterruptedException, ExecutionException {
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

        // TODO: javafx is still working with events, so some fields can't be accessed
        // this is dumb waiting for javafx fills the  fields
        Thread.sleep(2000);
    }

    public void runAndWait2(Runnable action) throws InterruptedException, ExecutionException {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get();

        // TODO: javafx is still working with events, so some fields can't be accessed
        // this is dumb waiting for javafx fills the  fields
        sleep(2000);
    }

    TRexPacketCraftingTool trex = new TRexPacketCraftingTool();
    FieldEditorController editorController = trex.getInjector().getInstance(FieldEditorController.class);
    URL resources;
    {
        try {
            resources = new URL(getClass().getResource("/"), "../../resources/test/");
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
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

    public void printNodesId() {
        NodeQuery query2 = lookup((Node t) -> {
            if (t.getId()!=null) {
                logger.debug("'" + t.getId() + "'");
                return true;
            }
            return false;
        });
    }

    void loadPcapFile(String filename) {
        loadPcapFileEx(filename, false);
    }

    void loadPcapFileEx(String filename, boolean mustfail) {
        final String[] error = {null};
        try {
            Assert.assertNotNull(editorController);
            runAndWait(() -> {
                try {
                    File file;
                    if (filename.startsWith("/")) {
                        file = new File(filename);
                    }
                    else {
                        file = new File(resources.getFile() + filename);
                    }
                    editorController.loadPcapFile(file, true);
                } catch (Exception e) {
                    error[0] = e.getMessage();
                }
            });
        } catch (InterruptedException e) {
            error[0] = e.getMessage();
        } catch (ExecutionException e) {
            error[0] = e.getMessage();
        }
        if (mustfail && error[0] == null) {
            fail("The test must fail, but it passed instead");
        }
        if (!mustfail && error[0] != null) {
            fail(error[0]);
        }
    }

    void savePcapFile(String filename) {
        savePcapFileEx(filename, false);
    }

    void savePcapFileEx(String filename, boolean mustfail) {
        final String[] error = {null};
        try {
            Assert.assertNotNull(editorController);
            runAndWait(() -> {
                try {
                    File file;
                    if (filename.startsWith("/")) {
                        file = new File(filename);
                    }
                    else {
                        file = new File(resources.getFile() + filename);
                    }
                    editorController.writeToPcapFile(file, true);
                } catch (Exception e) {
                    error[0] = e.getMessage();
                }
            });
        } catch (InterruptedException e) {
            error[0] = e.getMessage();
        } catch (ExecutionException e) {
            error[0] = e.getMessage();
        }
        if (mustfail && error[0] == null) {
            fail("The test must fail, but it passed instead");
        }
        if (!mustfail && error[0] != null) {
            fail(error[0]);
        }
    }

    void addLayer(String layerType) {
        //clickOn("Action");
        //clickOn("Add Protocol");
        clickOn(".layer-type-selector .arrow-button");
        clickOn(layerType);
        clickOn("Add");
    }

    void selectProtoType(String proto) {
        clickOn("#Ether-type");
        clickOn("#Ether-type");
        //push(CONTROL, DOWN);
        clickOn(proto);
    }

    void recalculateAutoValues() {
        clickOn("Action");
        clickOn("Recalculate auto-values");
    }

    void newDocument() {
        clickOn("File");
        clickOn("New Document");
    }

}
