package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.events.ProtocolExpandCollapseEvent;
import com.xored.javafx.packeteditor.metatdata.PacketTemplate;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.service.ConfigurationService.ApplicationMode.STANDALONE;

public class MenuController implements Initializable {

    public static final String EXIT_MENU_ITEM = "exit";
    private Logger logger= LoggerFactory.getLogger(MenuController.class);
    
    @Inject
    FieldEditorController controller;

    @Inject
    private EventBus eventBus;

    @FXML
    MenuBar applicationMenu;

    @FXML
    Menu fileMenu;
    
    @FXML
    Menu newTemplateMenu;
    
    @FXML
    MenuButton newTemplateMenuButton;

    @FXML
    Menu debugMenu;

    @FXML
    Button binaryModeOnBtn;

    @FXML
    Button abstractModeOnBtn;

    @FXML
    ComboBox<ProtocolMetadata> protocolSelector;
    
    @FXML
    Button appendProtocolBtn;
    
    @Inject
    @Named("resources")
    ResourceBundle resourceBundle;

    @Inject
    AppController appController;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (STANDALONE.equals(appController.getApplicationMode())) {
            addTemplates(newTemplateMenu.getItems());
        }
        addTemplates(newTemplateMenuButton.getItems());

        if (System.getenv("DEBUG") != null) {
            debugMenu.setVisible(true);
            binaryModeOnBtn.setVisible(true);
            abstractModeOnBtn.setVisible(true);
        }
        
        List<ProtocolMetadata> protocols = controller.getModel().getAvailableProtocolsToAdd(false);

        protocolSelector.setId("append-protocol-combobox");
        protocolSelector.getStyleClass().add("protocol-type-selector");
        protocolSelector.setEditable(true);
        protocolSelector.getItems().addAll(protocols);

        // Display only available protocols, but let user choose any
        List<String> protoIds = controller.getMetadataService().getProtocols().values().stream()
                .map(ProtocolMetadata::getId)
                .sorted()
                .collect(Collectors.toList());

        final AutoCompletionBinding<String> protoAutoCompleter = TextFields.bindAutoCompletion(protocolSelector.getEditor(), protoIds);
        
        protocolSelector.setOnShown((e) -> {
               protoAutoCompleter.dispose();
        });

        Consumer<Object> onAppendLayer = (o) -> {
            Object sel = protocolSelector.getSelectionModel().getSelectedItem();
            try {
                if (sel==null) {
                    sel = protocolSelector.getEditor().getText();
                }
                if (sel instanceof ProtocolMetadata) {
                    controller.getModel().addProtocol((ProtocolMetadata)sel);
                }
                else if (sel instanceof String) {
                    String selText = (String)sel;
                    ProtocolMetadata meta = protocols.stream().filter(
                            m -> m.getId().equals(selText) || m.getName().equals(selText)
                    ).findFirst().orElse(null);
                    if (meta != null) {
                        controller.getModel().addProtocol(meta);
                    } else {
                        controller.getModel().addProtocol(selText);
                    }
                }
            } catch(Exception e) {
                String selectedProtocolName = "unknown";
                if (sel instanceof ProtocolMetadata) {
                    selectedProtocolName = ((ProtocolMetadata)sel).getName();
                } else if (sel instanceof String) {
                    selectedProtocolName = (String) sel;
                }
//                Alert alert = new Alert(Alert.AlertType.ERROR);
//                alert.setHeaderText("Unable to add \""+ selectedProtocolName +"\" protocol.");
//                alert.initOwner(fieldEditorPane.getScene().getWindow());

//                alert.showAndWait();
            }
        };

        appendProtocolBtn.setOnAction(e -> onAppendLayer.accept(null));
        
        protocolSelector.setOnKeyReleased( e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                onAppendLayer.accept(null);
            }
        });
    }

    private void addTemplates(ObservableList<MenuItem> menuItems) {
        PacketTemplate.loadTemplates().forEach(templateFile -> {
            MenuItem menuItem = new MenuItem(templateFile.metadata.caption);
            menuItem.setOnAction(event -> controller.getModel().loadTemplate(templateFile));
            menuItems.add(menuItem);
        });
    }

    @FXML
    private void handleCloseAction() {
        appController.shutDown();
    }

    @FXML
    public void handleDeleteProtocolAction(ActionEvent actionEvent) {
        getModel().removeLast();
    }

    private FieldEditorModel getModel() { return controller.getModel(); }

    @FXML
    public void handleNewDocument(ActionEvent actionEvent) {
        getModel().newPacket();
    }

    @FXML
    public void handleOpenAction(ActionEvent actionEvent) {
        controller.showLoadDialog();
    }

    @FXML
    public void handleSaveAction(ActionEvent event) {
        controller.showSaveDialog();
    }

    @FXML
    public void handleRecalculateValues(ActionEvent actionEvent) {
        getModel().clearAutoFields();
    }

    @FXML
    public void handleUndo(ActionEvent actionEvent) {
        getModel().undo();
    }

    @FXML
    public void handleRedo(ActionEvent actionEvent){
        getModel().redo();
    }

    @FXML
    public void handleModeBinary(ActionEvent actionEvent) {
        getModel().setBinaryMode(true);
    }

    @FXML
    public void handleModeAbstract(ActionEvent actionEvent) {
        getModel().setBinaryMode(false);
    }

    @FXML
    public void handleCopyInstructions(ActionEvent event) {
        controller.copyInstructionsToClipboard();
    }
    
    public void handleExpandAll(ActionEvent actionEvent) {
        eventBus.post(new ProtocolExpandCollapseEvent(ProtocolExpandCollapseEvent.Action.EXPAND_ALL));
    }

    public void handleCollapseAll(ActionEvent actionEvent) {
        eventBus.post(new ProtocolExpandCollapseEvent(ProtocolExpandCollapseEvent.Action.COLLAPSE_ALL));
    }
}
