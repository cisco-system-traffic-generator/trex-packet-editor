package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.user.DocumentFile;
import com.xored.javafx.packeteditor.events.ProtocolExpandCollapseEvent;
import com.xored.javafx.packeteditor.events.RebuildViewEvent;
import com.xored.javafx.packeteditor.scapy.PacketData;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import com.xored.javafx.packeteditor.view.ConnectionErrorDialog;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FieldEditorController implements Initializable {

    public static final int PCAP_MAX_FILESIZE = 1048576;
    static Logger logger = LoggerFactory.getLogger(FieldEditorController.class);

    @FXML private StackPane  fieldEditorTopPane;
    @FXML private StackPane  fieldEditorPane;
    @FXML private ScrollPane fieldEditorScrollPane;

    @Inject
    FieldEditorModel model;
    
    @Inject
    IMetadataService metadataService;

    @Inject
    private PacketDataService packetController;
    
    @Inject
    FieldEditorView view;

    @Inject
    ConfigurationService configurationService;

    FileChooser fileChooser = new FileChooser();

    @Inject
    @Named("resources")
    private ResourceBundle resourceBundle;
    
    final KeyCombination SHORTCUT_N = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
    final KeyCombination SHORTCUT_O = new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN);
    final KeyCombination SHORTCUT_S = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
    final KeyCombination SHORTCUT_Z = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN);
    final KeyCombination SHORTCUT_R = new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);
    final KeyCombination SHORTCUT_D = new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN);
    
    public IMetadataService getMetadataService() {
        return metadataService;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        view.setParentPane(fieldEditorPane);
        if (packetController.isInitialized()) {
            if (configurationService.isStandaloneMode()) {
                Platform.runLater(this::newPacket);
            } else {
                view.showEmptyPacketContent();
            }
        } else {
            view.displayConnectionError();
        }
    }

    public void initAcceleratorsHandler(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (SHORTCUT_N.match(event)) {
                // TODO: add check for unsaved changes.
                newPacket();
                event.consume();
            }
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (SHORTCUT_O.match(event)) {
                showLoadDialog();
                event.consume();
            }
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (SHORTCUT_S.match(event)) {
                showSaveDialog();
                event.consume();
            }
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (SHORTCUT_Z.match(event)) {
                model.undo();
                event.consume();
            }
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (SHORTCUT_R.match(event)) {
                model.redo();
                event.consume();
            }
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (SHORTCUT_D.match(event)) {
                model.removeLast();
                event.consume();
            }
        });
    }
    
    public void showConnectionErrorDialog() {
        ConnectionErrorDialog dialog = new ConnectionErrorDialog();
        dialog.showAndWait();
    }

    public void refreshTitle() {
        String title = resourceBundle.getString("EDITOR_TITLE");
        if (model.getCurrentFile() != null) {
            title += " - " + model.getCurrentFile().getAbsolutePath();
        }

        if (configurationService.isStandaloneMode()) {
            ((Stage)fieldEditorPane.getScene().getWindow()).setTitle(title);
        }
    }

    @Subscribe
    public void handleProtocolExpandCollapseEvent(ProtocolExpandCollapseEvent event) {
        view.getProtocolTitledPanes().stream().filter(titledPane -> titledPane.isCollapsible()).forEach(titledPane ->
            titledPane.setExpanded(event.expandState())
        );
    }

    @Subscribe
    public void handleRebuildViewEvent(RebuildViewEvent event) {
        double val = fieldEditorScrollPane.getVvalue();

        // Workaround for flickering and saving Vscroll:
        // 1) take snapshot of top stackpane
        // 2) create image view with paddings
        // 3) add this image view to stackpane
        //
        // This image view hides scrollpane which is rebuilt
        // Then call view rebuild
        // And last, we set Vscroll and remove image view
        WritableImage snapImage = fieldEditorTopPane.snapshot(new SnapshotParameters(), null);
        ImageView snapView = new ImageView();
        snapView.setImage(snapImage);
        Insets insets = fieldEditorTopPane.getPadding();
        snapView.setViewport(new javafx.geometry.Rectangle2D(insets.getLeft(),
                insets.getTop(),
                snapImage.getWidth() - insets.getLeft() - insets.getRight(),
                snapImage.getHeight() - insets.getTop() - insets.getBottom()));
        fieldEditorTopPane.getChildren().add(snapView);
        // Rebuild content
        view.rebuild(event.getModel());
        
        Platform.runLater(()-> {
            // Save scroll position workaround: runLater inside runLater :)
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    fieldEditorScrollPane.setVvalue(val);
                    fieldEditorTopPane.getChildren().remove(snapView);
                });
            });
        });
    }

    public void showLoadDialog() {
        initFileChooser();
        fileChooser.setTitle(resourceBundle.getString("OPEN_DIALOG_TITLE"));
        File openFile = fileChooser.showOpenDialog(fieldEditorPane.getScene().getWindow());

        try {
            if (openFile != null) {
                if (openFile.getName().endsWith(".pcap")
                    && openFile.length() > PCAP_MAX_FILESIZE) {
                    showError("Pcap file size should be less than 1MB");
                    return;
                } else if (openFile.getName().endsWith(DocumentFile.FILE_EXTENSION)) {
                    model.loadDocumentFromFile(openFile);
                } else {
                    loadPcapFile(openFile);
                }
            }
            refreshTitle();
        } catch (Exception e) {
            showError(resourceBundle.getString("LOAD_ERROR"), e);
        }
    }

    public void loadPcapFile(File pcapfile) throws IOException {
        byte[] bytes = Files.toByteArray(pcapfile);
        model.setCurrentFile(pcapfile);
        refreshTitle();
        model.loadDocumentFromPcapData(packetController.read_pcap_packet(bytes));
        // Set window width to scene width
        fitSizeToScene();
    }

    public void loadPcapBinary(byte[] bytes) throws IOException {
        model.loadDocumentFromPcapData(packetController.reconstructPacketFromBinary(bytes));
    }

    public void writeToPcapFile(File file) {
        try {
            writeToPcapFile(file, model.getPkt(), false);
        } catch (Exception e) {
            // Something is wrong, must not be here
            logger.error(e.getMessage());
        }
    }

    public void writeToPcapFile(File file, boolean wantexception) throws Exception {
        writeToPcapFile(file, model.getPkt(), wantexception);
    }

    public void writeToPcapFile(File file, PacketData pkt, boolean wantexception) throws Exception {
        try {
            byte[] pcap_bin = packetController.write_pcap_packet(pkt.getPacketBytes());
            Files.write(pcap_bin, file);
        } catch (Exception e) {
            if (wantexception) {
                throw e;
            }
            else {
                showError(resourceBundle.getString("SAVE_ERROR"), e);
            }
        }
    }

    public void showError(String title) {
        showError(title, null);
    }

    public void showError(String title, Exception e) {
        logger.error("{}: {}", title, e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.initOwner(fieldEditorPane.getScene().getWindow());
        if (e != null ) {
            alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(title + ": " + e.getMessage())));
        }
        alert.showAndWait();
    }

    public void initFileChooser() {
        String docExt = "*"+DocumentFile.FILE_EXTENSION;
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("TRex Packet editor Files", docExt, "*.pcap", "*.cap"),
                new FileChooser.ExtensionFilter("Packet Editor Files", docExt),
                new FileChooser.ExtensionFilter("Pcap Files", "*.pcap", "*.cap"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File file = model.getCurrentFile();
        if (file != null) {
            fileChooser.setInitialDirectory(file.getParentFile());
            fileChooser.setInitialFileName(file.getName());
        }
    }

    public void showSaveDialog() {
        initFileChooser();
        fileChooser.setTitle(resourceBundle.getString("SAVE_DIALOG_TITLE"));
        java.io.File outFile = fileChooser.showSaveDialog(fieldEditorPane.getScene().getWindow());
        if (outFile != null) {
            try {
                if (outFile.getName().endsWith(DocumentFile.FILE_EXTENSION)) {
                    model.saveDocumentToFile(outFile);
                } else {
                    writeToPcapFile(outFile);
                }
            } catch (Exception e) {
                showError("Failed to save file", e);
            }
        }
    }

    public void selectField(CombinedField field) {
        model.setSelected(field);
    }

    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public void newPacket() {
        model.newPacket();
        // Set window width to scene width
        fitSizeToScene();
    }

    public FieldEditorModel getModel() { return model; }

    private void fitSizeToScene() {
        if (configurationService.isStandaloneMode()) {
            fieldEditorPane.getScene().getWindow().sizeToScene();
        }
    }

    public void loadUserModel(String userModelJSON) {
        model.loadDocumentFromJSON(userModelJSON);
    }

    public void reset() {
        model.reset();
        view.reset();
    }

    public String getBinaryPkt() {
        return getModel().getPkt().binary;
    }

    public Map<String, Object> getPktVmInstructions() {
        return getModel().getPkt().getPktVmInstructions();
    }

    public void copyInstructionsToClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        List<String> vmInstructions = getModel().getVmInstructions();
        if(vmInstructions.isEmpty()) {
            return;
        }
        String header = vmInstructions.get(0);
        int lastIdx = vmInstructions.size()-1;
        String footer = vmInstructions.get(lastIdx);
        vmInstructions.remove(lastIdx);
        vmInstructions.remove(0);
        String instructions = vmInstructions.stream()
                .collect(Collectors.joining(",\n"));
        content.putString(header+instructions+footer);
        clipboard.setContent(content);
    }
}
