package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.data.PacketEditorModel;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.user.DocumentFile;
import com.xored.javafx.packeteditor.events.*;
import com.xored.javafx.packeteditor.scapy.MethodNotFoundException;
import com.xored.javafx.packeteditor.scapy.PacketData;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import com.xored.javafx.packeteditor.service.ConfigurationService;
import com.xored.javafx.packeteditor.service.IMetadataService;
import com.xored.javafx.packeteditor.service.PacketDataService;
import com.xored.javafx.packeteditor.view.ConnectionErrorDialog;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import com.xored.javafx.packeteditor.view.FieldEngineView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.HiddenSidesPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FieldEditorController implements Initializable {

    public static final int PCAP_MAX_FILESIZE = 1048576;
    static Logger logger = LoggerFactory.getLogger(FieldEditorController.class);

    @FXML private BorderPane fieldEditorBorderPane;
    @FXML private FlowPane   fieldEditorTopPane;
    @FXML private StackPane  fieldEditorCenterPane;
    @FXML private StackPane  fieldEditorBottomPane;
    @FXML private ScrollPane fieldEditorScrollPane;

    @Inject
    ScapyServerClient scapy;

    @Inject
    private MenuControllerEditor menuControllerEditor;

    @Inject
    PacketEditorModel model;
    
    @Inject
    IMetadataService metadataService;

    @Inject
    private PacketDataService packetController;
    
    @Inject
    FieldEditorView fieldEditorView;
    
    @Inject
    FieldEngineView fieldEngineView;

    @Inject
    ConfigurationService configurationService;

    @Inject
    EventBus eventBus;

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
    private boolean viewOnly = false;

    public IMetadataService getMetadataService() {
        return metadataService;
    }

    // The following initialize() method is intended for both FieldEngine.fxml and FieldEditor.fxml
    // So, some injected fields are null at first invocation
    // Therefore, we check that enigines fields are null and assume that this a first call
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fieldEditorView.setRootPane(fieldEditorCenterPane);
        fieldEditorView.setBreadCrumbPane(fieldEditorTopPane);
        fieldEditorView.setBottomPane(fieldEditorBottomPane);

        if (packetController.isInitialized()) {
            if (configurationService.isStandaloneMode()) {
                Platform.runLater(this::newPacket);
            } else {
                fieldEditorView.showEmptyPacketContent();
            }
        } else {
            fieldEditorView.showNoConnectionContent();
        }
    }

    public void setViewOnly(boolean viewOnly) {
        this.viewOnly = viewOnly;
    }

    public FieldEditorView getFieldEditorView() {
        return fieldEditorView;
    }

    public void setFieldEditorBorderPane(BorderPane fieldEditorBorderPane) {
        this.fieldEditorBorderPane = fieldEditorBorderPane;
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
            ((Stage) fieldEditorCenterPane.getScene().getWindow()).setTitle(title);
        }
    }

    @Subscribe
    public void handleProtocolExpandCollapseEvent(ProtocolExpandCollapseEvent event) {
        List<TitledPane> panes = fieldEditorView.getProtocolTitledPanes();
        if (panes.isEmpty())
            return;
        TitledPane lastPane = panes.get(panes.size() - 1);
        panes.stream().filter(titledPane -> titledPane.isCollapsible()).forEach(titledPane -> {
            switch(event.getAction()) {
                case EXPAND_ALL:
                    titledPane.setExpanded(true);
                    break;
                case COLLAPSE_ALL:
                    titledPane.setExpanded(false);
                    break;
                case EXPAND_ONLY_LAST:
                    titledPane.setExpanded(titledPane == lastPane);
                    break;
            }
        }
        );
    }

    @Subscribe
    public void handleRebuildViewEvent(RebuildViewEvent event) {
        if (fieldEditorScrollPane == null) {
            return;
        }

        if (viewOnly && packetController.isInitialized()) {
            fieldEditorView.rebuild();
            fieldEngineView.rebuild();
            return;
        }
        
        double val = fieldEditorScrollPane.getVvalue();

        // Workaround for flickering and saving Vscroll:
        // 1) take snapshot of top stackpane
        // 2) create image view with paddings
        // 3) add this image view to stackpane
        //
        // This image view hides scrollpane which is rebuilt
        // Then call view rebuild
        // And last, we set Vscroll and remove image view
        WritableImage snapImage = fieldEditorBorderPane.snapshot(new SnapshotParameters(), null);
        ImageView snapView = new ImageView();
        snapView.setImage(snapImage);
        Insets insets = fieldEditorBorderPane.getPadding();
        snapView.setViewport(new javafx.geometry.Rectangle2D(insets.getLeft(),
                insets.getTop(),
                snapImage.getWidth() - insets.getLeft() - insets.getRight(),
                snapImage.getHeight() - insets.getTop() - insets.getBottom()));
        fieldEditorBorderPane.getChildren().add(snapView);

        // Rebuild content
        if (packetController.isInitialized()) {
            fieldEditorView.rebuild();
            fieldEngineView.rebuild();
        }

        Platform.runLater(()-> {
            // Save scroll position workaround: runLater inside runLater :)
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    fieldEditorScrollPane.setVvalue(val);
                    fieldEditorBorderPane.getChildren().remove(snapView);
                });
            });
        });
    }

    public String createNewTemplateDialog() {
        // Add templates from templates dir
        TextInputDialog dialog = new TextInputDialog("NewTemplate");
        dialog.setTitle("Save as template");
        dialog.setHeaderText("Please enter template name");
        dialog.setContentText("");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            return result.get();
        }
        return null;
    }

    public boolean createFileOverwriteDialog(File f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Save as template");
        alert.setHeaderText("File " + f.getName() + " exists");
        alert.setContentText("Do you want to overwrite it ?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.get() == ButtonType.OK;
    }

    public void showLoadDialog() {
        initFileChooser();
        fileChooser.setTitle(resourceBundle.getString("OPEN_DIALOG_TITLE"));
        File openFile = fileChooser.showOpenDialog(fieldEditorCenterPane.getScene().getWindow());

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
        if (packetController.isInitialized()) {
            model.loadDocumentFromPcapData(packetController.read_pcap_packet(writePcapPacket(bytes)));
        }
    }

    public void loadVmRaw(String vmRaw) {
        if (packetController.isInitialized()) {
            try {
                String hlvm = packetController.decompileVmRaw(model.getPkt().getPacketBytes(), vmRaw);
                model.loadHighLevelVmInstructions(hlvm);
            } catch (Exception e) {
                String content = "Unable to decomple raw VM instructions\n" +
                        "make sure you use latest TRex server and Scapy server version\n" +
                        "Error: " +
                        e.getMessage();
                showWarning(content);
            }
        }
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
            byte[] pcap_bin = writePcapPacket(pkt.getPacketBytes());
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
    
    public byte[] writePcapPacket(byte[] binaryData) {
        return packetController.write_pcap_packet(binaryData);
    }

    public void showError(String title) {
        showError(title, null);
    }

    public void showError(String title, Exception e) {
        logger.error("{}: {}", title, e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.initOwner(fieldEditorCenterPane.getScene().getWindow());
        if (e != null ) {
            alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(title + ": " + e.getMessage())));
        }
        alert.showAndWait();
    }

    public void showWarning(String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        Text text = new Text(content);
        text.setWrappingWidth(350);
        text.setFontSmoothingType(FontSmoothingType.LCD);

        HBox container = new HBox();
        container.getChildren().add(text);
        alert.getDialogPane().setContent(container);

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
        java.io.File outFile = fileChooser.showSaveDialog(fieldEditorCenterPane.getScene().getWindow());
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

    public void connect() {
        eventBus.post(new ScapyClientNeedConnectEvent());
    }

    public PacketEditorModel getModel() { return model; }

    private void fitSizeToScene() {
        if (configurationService.isStandaloneMode()) {
            fieldEditorCenterPane.getScene().getWindow().sizeToScene();
        }
    }

    /**
     * Load user model from JSON encoded in base64.
     * This method is used in trex-stateless-gui application.
     * 
     * @param base64JSONUserModel
     */
    public void loadUserModel(String base64JSONUserModel) {
        reset();
        if (packetController.isInitialized()) {
            model.loadDocumentFromJSON(base64JSONUserModel);
        }
    }
    /**
     * Load simple JSON encoded user model.
     */
    public void loadSimpleUserModel(String json) {
        reset();
        model.loadSimpleUserModel(json);
    }

    public void reset() {
        model.reset();
        fieldEditorView.reset(packetController.isInitialized());
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

    /****************************************************/
    @FXML
    private HiddenSidesPane pane;

    @FXML
    private Label pinLabel;
    @FXML
    private Label pinLabel2;

    @FXML
    public void handleMouseClickedX(MouseEvent event) {
        if (pane.getPinnedSide() != null) {
            pinLabel.setText("(unpinned)");
            pane.setPinnedSide(null);
        } else {
            pinLabel.setText("(pinned)");
            pane.setPinnedSide(Side.TOP);
        }
    }

    @FXML
    public void handleMouseClickedX2(MouseEvent event) {
        if (pane.getPinnedSide() != null) {
            pinLabel2.setText("(unpinned)");
            pane.setPinnedSide(null);
        } else {
            pinLabel2.setText("(pinned)");
            pane.setPinnedSide(Side.BOTTOM);
        }
    }

    public List<JsonObject> getTemplates() {
        if (!scapy.isConnected()) {
            return null;
        }
        try {
            return scapy.getTemplates();
        } catch (MethodNotFoundException e) {
            return null;
        }
    }

    public String getTemplate(JsonObject t) {
        if (!scapy.isConnected()) {
            return null;
        }
        try {
            return scapy.getTemplate(t);
        } catch (MethodNotFoundException e) {
            return null;
        }
    }

    @Subscribe
    public void handleScapyConnectedEventEditor(ScapyClientConnectedEvent event) {
        initTemplateMenu();
    }

    @Subscribe
    public void handleNeedToUpdateTemplateMenuEditor(NeedToUpdateTemplateMenu event) {
        initTemplateMenu();
    }

    private void initTemplateMenu() {
        menuControllerEditor.initTemplateMenu();
    }

    public String getFieldEngineError() {
        return model.getFieldEngineError();
    }

    public void loadTemplateFromScapy(String templateId) {
        JsonObject template = new JsonObject();
        template.add("id", new JsonPrimitive(templateId));
        String templateBase64 = getTemplate(template);
        model.loadDocumentFromJSON(templateBase64);
    }

    public void setFieldEditorScrollPane(ScrollPane fieldEditorScrollPane) {
        this.fieldEditorScrollPane = fieldEditorScrollPane;
    }

    public boolean isViewOnly() {
        return viewOnly;
    }
}
