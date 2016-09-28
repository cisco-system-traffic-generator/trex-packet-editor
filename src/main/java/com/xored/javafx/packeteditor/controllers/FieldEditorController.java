package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.Field;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.data.PacketDataController;
import com.xored.javafx.packeteditor.events.RebuildViewEvent;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FieldEditorController implements Initializable {

    static Logger logger = LoggerFactory.getLogger(FieldEditorController.class);
    
    @FXML private StackPane fieldEditorPane;
    @FXML private ScrollPane scrollPane;
    
    @Inject
    FieldEditorModel model;
    
    @Inject
    IMetadataService metadataService;

    @Inject
    private PacketDataController packetController;
    
    @Inject
    FieldEditorView view;

    FileChooser fileChooser = new FileChooser();
    
    private ResourceBundle resourceBundle;

    public IMetadataService getMetadataService() {
        return metadataService;
    }
    
    public void addProtocol(ProtocolMetadata protocolMetadata) {
        model.addProtocol(protocolMetadata);
    }
    
    public List<ProtocolMetadata> getAvailbleProtocolsToAdd() {
        return model.getAvailableProtocolsToAdd();
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        packetController.init();
        view.setParentPane(fieldEditorPane);
        model.setMetadataService(metadataService);

        packetController.addObserver((source,params)->{
            String title = "Packet Editor";
            if (packetController.getCurrentFile() != null) {
                title = title + " - " + packetController.getCurrentFile().getAbsolutePath();
            }

            ((Stage)fieldEditorPane.getScene().getWindow()).setTitle(title);
        });

    }

    @Subscribe
    public void handleRebuildViewEvent(RebuildViewEvent event) {
        ScrollBar scrollBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
        double scrollBarValue = scrollBar.getValue();
        view.rebuild(event.getProtocols());
        Platform.runLater(()-> scrollBar.setValue(scrollBarValue));
    }

    public void clearLayers() {
        model.deleteAllProtocols();
    }

    public void removeLast() {
        model.removeLast();
    }

    public void showLoadDialog() {
        fileChooser.setTitle(resourceBundle.getString("openDialogTitle"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pcap Files", "*.pcap", "*.cap"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        initFileChooser();
        java.io.File pcapfile = fileChooser.showOpenDialog(fieldEditorPane.getScene().getWindow());
        if (pcapfile != null) {
            try {
                packetController.loadPcapFile(pcapfile);
            } catch (Exception e) {
                showError("Failed to load pcap file", e);
            }
        }
    }

    void showError(String title, Exception e) {
        logger.error("{}: {}", title, e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.initOwner(fieldEditorPane.getScene().getWindow());
        alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(title + ": " + e.getMessage())));
        alert.showAndWait();
    }

    public void initFileChooser() {
        File file = packetController.getCurrentFile();
        if (file != null) {
            fileChooser.setInitialDirectory(file.getParentFile());
            fileChooser.setInitialFileName(file.getName());
        }
    }

    public void showSaveDialog() {
        fileChooser.setTitle("Save to Pcap File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pcap Files", "*.pcap", "*.cap"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        initFileChooser();
        java.io.File pcapfile = fileChooser.showSaveDialog(fieldEditorPane.getScene().getWindow());
        if (pcapfile != null) {
            try {
                packetController.writeToPcapFile(pcapfile);
            } catch (Exception e) {
                showError("Failed to save pcap file", e);
            }
        }
    }

    public void selectField(Field field) {
        model.setSelected(field);
    }

    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }
}
