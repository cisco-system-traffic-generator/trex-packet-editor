package com.xored.javafx.packeteditor.controllers;

import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.PacketDataController;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.service.IMetadataService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class MenuController {

    private Logger logger= LoggerFactory.getLogger(MenuController.class);
    
    @Inject
    PacketDataController packetController;

    @Inject
    FieldEditorController2 controller;

    @FXML
    MenuBar applicationMenu;

    private ChoiceDialog<ProtocolMetadata> dialog = new ChoiceDialog<>();
    
    @FXML
    private void handleCloseAction() {
        logger.info("Closing application");
        System.exit(0);
    }
    
    @FXML
    private void handleAddProtocolAction(ActionEvent actionEvent) {
        IMetadataService metadataService = controller.getMetadataService();
        
        List<ProtocolMetadata> items = controller.getAvailbleProtocolsToAdd();
        
        dialog.getItems().clear();
        dialog.getItems().addAll(items);
        if(items.size() == 1) {
            dialog.setSelectedItem(items.get(0));
        }
        
        dialog.setTitle("Add layer");
        dialog.setContentText("Select protocol:");
        
        // TODO: Add proper protocol icon to dialog
        // dialog.setGraphic(new ImageView(this.getClass().getResource("protocol-image.png").toString()));
        
        Optional<ProtocolMetadata> result = dialog.showAndWait();
        if(result.isPresent()) {
            controller.addProtocol(result.get());
        }
    }
    @FXML
    public void handleClearAction(ActionEvent actionEvent) {
        controller.clearLayers();
    }
    @FXML
    public void handleDeleteProtocolAction(ActionEvent actionEvent) {
        controller.removeLast();
    }

    public void handleLoadAction(ActionEvent actionEvent) {
        controller.showLoadDialog();
    }

    @FXML
    public void handleNewDocument(ActionEvent actionEvent) {
        packetController.newPacket();
    }

    public void handleSaveAction(ActionEvent event) {
        controller.showSaveDialog();
    }
}
