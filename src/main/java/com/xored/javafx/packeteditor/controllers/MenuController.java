package com.xored.javafx.packeteditor.controllers;

import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.service.IMetadataService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MenuController {

    private Logger logger= LoggerFactory.getLogger(MenuController.class);
    
    @Inject
    IMetadataService metadataService;
    
    @Inject
    FieldEditorModel fieldEditorModel;
    
    @FXML
    MenuBar applicationMenu;
    
    @FXML
    private void handleCloseAction() {
        logger.info("Closing application");
        System.exit(0);
    }
    
    @FXML
    private void handleAddProtocolAction(ActionEvent actionEvent) {
        Map<String, ProtocolMetadata> protocolMetadataMap = metadataService.getProtocols();
        
        // TODO: Display available payloads of current protocol
        List<ProtocolMetadata> protocolMetadataList = metadataService.getProtocols().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
        
        ChoiceDialog<ProtocolMetadata> dialog = new ChoiceDialog<>(protocolMetadataMap.get("Ether"), protocolMetadataList);
        dialog.setTitle("Available Protocols");
        dialog.setContentText("Select protocol:");
        
        // TODO: Add proper protocol icon to dialog
        // dialog.setGraphic(new ImageView(this.getClass().getResource("protocol-image.png").toString()));
        

        Optional<ProtocolMetadata> result = dialog.showAndWait();
        
        fieldEditorModel.addProtocol(result.get());
        
        // Get model. Add new protocol to model
    }
    @FXML
    public void handleBuildPacketAction(ActionEvent actionEvent) {
        logger.info("Packet built :)");
    }
}
