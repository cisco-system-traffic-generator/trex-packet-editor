package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.events.NeedToUpdateTemplateMenu;
import com.xored.javafx.packeteditor.events.ScapyClientConnectedEvent;
import com.xored.javafx.packeteditor.service.PacketDataService;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class FieldEngineController extends FieldEditorController {


    @FXML private StackPane  fieldEngineCenterPane;
    @FXML private ScrollPane fieldEngineScrollPane;
    @FXML private VBox       fieldEngineTopPane;
    @FXML private VBox       fieldEngineBottomPane;


    @Inject
    private PacketDataService packetController;

    @Inject
    private MenuControllerEngine menuControllerEngine;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (fieldEngineCenterPane != null) {
            fieldEngineView.setRootPane(fieldEngineCenterPane);
            fieldEngineView.setTopPane(fieldEngineTopPane);
            fieldEngineView.setBottomPane(fieldEngineBottomPane);
            fieldEngineView.setScrollPane(fieldEngineScrollPane);

            if (!packetController.isInitialized()) {
                fieldEngineView.showNoConnectionContent();
            }
        }
    }

    @Subscribe
    public void handleScapyConnectedEventEngine(ScapyClientConnectedEvent event) {
        initTemplateMenu();
    }

    @Subscribe
    public void handleNeedToUpdateTemplateMenuEngine(NeedToUpdateTemplateMenu event) {
        initTemplateMenu();
    }

    private void initTemplateMenu() {
        menuControllerEngine.initTemplateMenu();
    }

}
