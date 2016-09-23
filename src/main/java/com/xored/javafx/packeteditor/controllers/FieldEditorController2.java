package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.data.PacketDataController;
import com.xored.javafx.packeteditor.data.Protocol;
import com.xored.javafx.packeteditor.events.RebuildViewEvent;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FieldEditorController2 implements Initializable {

    static Logger logger = LoggerFactory.getLogger(FieldEditorController2.class);
    
    @FXML private StackPane fieldEditorPane;
    
    @Inject
    FieldEditorModel model;
    
    @Inject
    IMetadataService metadataService;

    @Inject
    private PacketDataController packetController;
    
    @Inject
    FieldEditorView view;

    public IMetadataService getMetadataService() {
        return metadataService;
    }
    
    public void addProtocol(ProtocolMetadata protocolMetadata) {
        model.addProtocol(protocolMetadata);
    }
    
    public Protocol getCurrentProtocol() {
        return model.getCurrentProtocol();
    }
    
    public List<ProtocolMetadata> getAvailbleProtocolsToAdd() {
        return model.getAvailableProtocolsToAdd();
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        packetController.init();
        view.setParentPane(fieldEditorPane);
        model.setMetadataService(metadataService);
    }

    @Subscribe
    public void handleRebuildViewEvent(RebuildViewEvent event) {
        view.rebuild(event.getProtocols());
    }

    public void clearLayers() {
        model.deleteAllProtocols();
    }
    public void removeLast() {
        model.removeLast();
    }

    public void showLoadDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Pcap File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pcap Files", "*.pcap", "*.cap"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
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

    public void showSaveDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save to Pcap File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pcap Files", "*.pcap", "*.cap"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        java.io.File pcapfile = fileChooser.showSaveDialog(fieldEditorPane.getScene().getWindow());
        if (pcapfile != null) {
            try {
                packetController.writeToPcapFile(pcapfile);
            } catch (Exception e) {
                showError("Failed to save pcap file", e);
            }
        }
    }
}
