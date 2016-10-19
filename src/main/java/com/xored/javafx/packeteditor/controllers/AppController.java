package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.IMetadataService;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AppController implements Initializable {

    private Logger logger= LoggerFactory.getLogger(AppController.class);

    @Inject
    private IMetadataService metadataService;

    @Inject
    private FieldEditorController editorController;

    @Inject
    private EventBus eventBus;

    @Inject
    private FieldEditorModel model;

    @Inject
    private ConfigurationService configurationService;
    
    @Inject
    private ScapyServerClient scapyServerClient;
    
    private Stage mainStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        metadataService.initialize();
        eventBus.register(editorController);
        eventBus.register(model);
    }
    
    public void terminate() {
        logger.info("Closing application");
        switch (configurationService.getApplicationMode()) {
            case STANDALONE:
                System.exit(0);
                break;
            case EMBEDDED:
                scapyServerClient.closeConnection();
        }
    }

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    public void shutDown() {
        mainStage.fireEvent(
                new WindowEvent(
                        mainStage,
                        WindowEvent.WINDOW_CLOSE_REQUEST
                )
        );
    }
}
