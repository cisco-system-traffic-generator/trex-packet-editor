package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.PacketEditorModel;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.ConfigurationService.ApplicationMode;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppController {

    private Logger logger= LoggerFactory.getLogger(AppController.class);

    @Inject
    private IMetadataService metadataService;

    @Inject
    private FieldEditorController editorController;

    @Inject
    private FieldEngineController engineController;

    @Inject
    private EventBus eventBus;

    @Inject
    private PacketEditorModel model;

    @Inject
    private ConfigurationService configurationService;
    
    @Inject
    private PacketDataService packetDataService;

    private Stage mainStage;

    @Inject
    public void initEventBus() {
        registerEventBusHandler(packetDataService);
        registerEventBusHandler(metadataService);
        registerEventBusHandler(editorController);
        registerEventBusHandler(engineController);
        registerEventBusHandler(model);
    }
    
    public void terminate() {
        logger.info("Closing application");
        switch (configurationService.getApplicationMode()) {
            case STANDALONE:
                System.exit(0);
                break;
            case EMBEDDED:
                packetDataService.closeConnection();
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
    
    public ApplicationMode getApplicationMode() {
        return configurationService.getApplicationMode();
    }

    public ConfigurationService getConfigurations() {
        return configurationService;
    }

    public void registerEventBusHandler(Object handler) {
        eventBus.register(handler);
    }
}
