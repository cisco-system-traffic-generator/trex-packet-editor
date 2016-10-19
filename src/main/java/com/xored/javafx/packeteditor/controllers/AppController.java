package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.IMetadataService;
import javafx.application.Platform;
import javafx.fxml.Initializable;
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
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        metadataService.initialize();
        eventBus.register(editorController);
        eventBus.register(model);
    }
    
    public void shutDown() {
        logger.info("Closing application");
        switch (configurationService.getApplicationMode()) {
            case STANDALONE:
                System.exit(0);
                break;
            case EMBEDDED:
                Platform.exit();
        }
    }
}
