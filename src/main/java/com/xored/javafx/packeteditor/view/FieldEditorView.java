package com.xored.javafx.packeteditor.view;

import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.controls.ProtocolField;
import com.xored.javafx.packeteditor.controls.TitledPaneCustomCaption;
import com.xored.javafx.packeteditor.data.PacketEditorModel;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocol;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import com.xored.javafx.packeteditor.scapy.TCPOptionsData;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.controlsfx.control.BreadCrumbBar;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.*;
import static javafx.scene.input.KeyCode.ENTER;

public class FieldEditorView {
    @Inject
    protected FieldEditorController controller;

    protected Pane rootPane;

    protected Logger logger = LoggerFactory.getLogger(FieldEditorView.class);

    @Inject
    @Named("resources")
    protected ResourceBundle resourceBundle;

    @Inject
    protected Injector injector;
    private AutoCompletionBinding<String> protoAutoCompleter;

    public List<TitledPane> getProtocolTitledPanes() {
        return protocolTitledPanes;
    }

    private List<TitledPane> protocolTitledPanes = new ArrayList<>();

    // For even/odd background, this is NOT any real field index
    static protected int oddIndex = 0;

    // Current selected field
    static protected HBox selected_row = null;

    static protected void setSelectedRow(HBox row) {
        if (selected_row != row) {
            if (selected_row != null) {
                selected_row.setStyle("-fx-background-color: -trex-field-row-background;");
            }
            selected_row = row;
            selected_row.setStyle("-fx-background-color: -trex-field-row-background-selected;");
        }
    }

    public static void initCss(Scene scene) {
        scene.getStylesheets().add(ClassLoader.getSystemResource("styles/modena-packet-editor.css").toExternalForm());

        Set<String> fontFamilies = javafx.scene.text.Font.getFamilies().stream().collect(Collectors.toSet());

        // Try choose best available fonts with a fallback
        String fontsCssFile;
        if (fontFamilies.contains("Menlo")) {
            fontsCssFile = "main-font-menlo.css";
        } else if (fontFamilies.contains("Lucida Console")) {
            fontsCssFile = "main-font-lucida-console.css";
        } else if (fontFamilies.contains("Consolas")) {
            fontsCssFile = "main-font-consolas.css";
        } else if (fontFamilies.contains("Courier New")) {
            fontsCssFile = "main-font-courier-new.css";
        } else {
            fontsCssFile = "main-font-monospace.css";
        }

        scene.getStylesheets().add(ClassLoader.getSystemResource("styles/" + fontsCssFile).toExternalForm());

        if (System.getenv("DEBUG") == null) {
            scene.getStylesheets().add(ClassLoader.getSystemResource("styles/main-narrow.css").toExternalForm());
        } else {
            // use css from source file to utilize JavaFX css auto-reload
            String cssSource = "file://" + new File("src/main/resources/styles/main-narrow.css").getAbsolutePath();
            scene.getStylesheets().add(cssSource);
        }
    }

    public void setRootPane(Pane rootPane) {
        this.rootPane = rootPane;
    }

    private MenuItem addMenuItem(ContextMenu ctxMenu, String text, EventHandler<ActionEvent> action) {
        MenuItem menuItem = new MenuItem();
        menuItem.setText(text);
        menuItem.setOnAction(action);
        ctxMenu.getItems().add(menuItem);
        return menuItem;
    }

    public TitledPane buildLayer(LayerContext layerContext) {
        TitledPane layerPane = new TitledPane();
        layerPane.setId(layerContext.getLayerId());
        
        layerPane.getStyleClass().add(layerContext.getStyleClass());
        
        GridPane layerContent = new GridPane();
        layerContent.getStyleClass().add("protocolgrid");

        int layerRowIdx = 0;

        oddIndex = 0;
        for(Node row : layerContext.getRows()) {
            layerContent.add(row, 0, layerRowIdx++);
        }

        String layerTitle = layerContext.getTitle();
        layerPane.setText(layerTitle);
        layerPane.setContent(layerContent);
        layerContext.configureLayerExpandCollapse(layerPane);
        layerPane.setContextMenu(layerContext.getContextMenu());
        return layerPane;
    }
    
    protected List<Node> buildLayerRows(CombinedProtocol protocol) {
        List<Node> rows = new ArrayList<>();
        protocol.getFields().stream().forEach(field -> {
            rows.add(buildFieldRow(field));
            FieldType type = field.getType();
            if(BITMASK.equals(type)) {
                rows.addAll(createBitFlagRows(field));
            }
            if(TCP_OPTIONS.equals(type) && field.getScapyFieldData() != null) {
                rows.addAll(createTCPOptionRows(field));
            }
        });
        return rows;
    }
    
    protected void configureExpandCollapse(TitledPane layerPane, CombinedProtocol protocol) {
        UserProtocol userProtocol = protocol.getUserProtocol();
        if(userProtocol != null) {
            layerPane.setExpanded(!userProtocol.isCollapsed());
            layerPane.expandedProperty().addListener(val->
                    userProtocol.setCollapsed(!layerPane.isExpanded())
            );
        }
    }
    
    protected ContextMenu getLayerContextMenu(CombinedProtocol protocol) {
        UserProtocol userProtocol = protocol.getUserProtocol();    
        PacketEditorModel model = getModel();
        ContextMenu layerCtxMenu = null;
        if (!model.isBinaryMode() && model.getUserModel().getProtocolStack().indexOf(userProtocol) > 0) {
            layerCtxMenu = new ContextMenu();
            addMenuItem(layerCtxMenu, "Move Layer Up", e -> model.moveLayerUp(userProtocol));
            addMenuItem(layerCtxMenu, "Move Layer Down", e -> model.moveLayerDown(userProtocol));
            addMenuItem(layerCtxMenu, "Delete layer", e -> model.removeLayer(userProtocol));
        }
        return layerCtxMenu;
    }
    
    private String getLayerId(CombinedProtocol protocol) {
        return protocol.getPath().stream().collect(Collectors.joining("-"));
    }
    
    protected String getLayerStyleClass(CombinedProtocol protocol) {
        UserProtocol userProtocol = protocol.getUserProtocol();
        ProtocolData protocolData = protocol.getScapyProtocol();
        if (userProtocol != null && protocolData != null
            && ( protocolData.isInvalidStructure() || protocolData.protocolRealIdDifferent())) {
            return "invalid-layer";
        }
        return "";
    }
    
    protected String getLayerTitle (CombinedProtocol protocol) {
        String title = protocol.getMeta().getName();
        UserProtocol userProtocol = protocol.getUserProtocol();
        ProtocolData protocolData = protocol.getScapyProtocol();
        String subtype = null;
        if (userProtocol != null && protocolData == null) {
            subtype = "as Payload";
        } else if (userProtocol != null && protocolData != null && protocolData.isInvalidStructure()) {
            if (protocolData.getRealId() == null) {
                subtype = "as Payload";
            } else {
                subtype = "as " + protocolData.getRealId();
            }
        } else if (userProtocol != null && protocolData != null && protocolData.protocolRealIdDifferent() ) {
            subtype = "as " + protocolData.getRealId();
        }

        if (subtype != null) {
            title = title + " " + subtype;
        }
        return title;
    }

    private Node buildProtocolStructureLayer() {
        BreadCrumbBar<UserProtocol> pktStructure = new BreadCrumbBar<>();

        pktStructure.setAutoNavigationEnabled(false);
        pktStructure.setSelectedCrumb(buildProtocolStructure());

        pktStructure.setOnCrumbAction((e) -> {
            BreadCrumbBar.BreadCrumbActionEvent<UserProtocol> event = e;
            UserProtocol protocol = e.getSelectedCrumb().getValue();
            Stack<UserProtocol> protocols = getModel().getUserModel().getProtocolStack();
            protocols.forEach(p -> p.setCollapsed(p != protocol));
            rebuild();
        });

        BorderPane pktStructureContent = new BorderPane();
        pktStructureContent.setLeft(pktStructure);

        TitledPaneCustomCaption pktStructurePane = new TitledPaneCustomCaption();
        pktStructurePane.setCollapsible(false);
        pktStructurePane.setText("Packet Structure");
        pktStructurePane.setContent(pktStructureContent);

        return pktStructurePane;
    }

    private TreeItem<UserProtocol> buildProtocolStructure() {
        Stack<UserProtocol> protocols = getModel().getUserModel().getProtocolStack();
        TreeItem<UserProtocol> currentProto = null;
        for(UserProtocol protocol : protocols) {
            if(currentProto == null) {
                currentProto = new TreeItem<UserProtocol>(protocol);
            } else {
                TreeItem<UserProtocol> child = new TreeItem<UserProtocol>(protocol);
                currentProto.getChildren().add(child);
                currentProto = child;
            }
        }
        return currentProto;
    }

    public TitledPane buildFETitlePane(String title, Node content) {
        TitledPane pane = new TitledPane();
        pane.setText(title);
        pane.getStyleClass().add("append-protocol");
        pane.setId("fe-parameters-pane");
        pane.setContent(content);
        pane.setCollapsible(false);
        return pane;
    }

    public void rebuild() {
        try {
            protocolTitledPanes = getModel().getCombinedProtocolModel().getProtocolStack().stream()
                    .map(this::buildLayer)
                    .collect(Collectors.toList());

            protocolTitledPanes.add(0, (TitledPane) buildProtocolStructureLayer());
            protocolTitledPanes.add(buildAppendProtocolPane());
            VBox protocolsPaneVbox = new VBox();
            protocolsPaneVbox.getChildren().setAll(protocolTitledPanes);
            rootPane.getChildren().setAll(protocolsPaneVbox);
        } catch(Exception e) {
            logger.error("Error occurred during rebuilding view. Error {}", e);
        }
    }

    private TitledPane buildLayer(CombinedProtocol protocol) {
        LayerContext layerContext = new LayerContext() {
            @Override
            public String getLayerId() {
                return protocol.getPath().stream().collect(Collectors.joining("-")) + "-pane";
            }
            @Override
            public String getTitle() {
                return getLayerTitle(protocol);
            }
            @Override
            public String getStyleClass() {
                return getLayerStyleClass(protocol);
            }
            @Override
            public List<Node> getRows() {
                return buildLayerRows(protocol);
            }
            @Override
            public ContextMenu getContextMenu() {
                return getLayerContextMenu(protocol);
            }
            @Override
            public boolean isCollapsed() {
                return false;
            }
            @Override
            public void setCollapsed(boolean collapsed) {
                protocol.getUserProtocol().setCollapsed(collapsed);
            }
            @Override
            public void configureLayerExpandCollapse(TitledPane layerPane) {
                configureExpandCollapse(layerPane, protocol);
            }
        };
        
        return buildLayer(layerContext);
    }

    public TitledPane buildAppendProtocolPane() {
        List<ProtocolMetadata> protocols = controller.getModel().getAvailableProtocolsToAdd(false);
        ComboBox<ProtocolMetadata> cb = new ComboBox<>();
        cb.setId("append-protocol-combobox");
        cb.getStyleClass().add("protocol-type-selector");
        cb.setEditable(true);
        cb.getItems().addAll(protocols);

        // Display only available protocols, but let user choose any
        List<String> protoIds = controller.getMetadataService().getProtocols().values().stream()
                .map(ProtocolMetadata::getId)
                .sorted()
                .collect(Collectors.toList());

        protoAutoCompleter = TextFields.bindAutoCompletion(cb.getEditor(), protoIds);
        cb.setOnHidden((e) -> {
            if (protoAutoCompleter == null) {
                protoAutoCompleter = TextFields.bindAutoCompletion(cb.getEditor(), protoIds);
            }
        });

        cb.setOnShown((e) -> {
            if (protoAutoCompleter!=null) {
                protoAutoCompleter.dispose();
                protoAutoCompleter = null;
            }
        });

        Button addBtn = new Button();
        addBtn.setId("append-protocol-button");
        addBtn.setText("Append layer");

        Consumer<Object> onAppendLayer = (o) -> {
            Object sel = cb.getSelectionModel().getSelectedItem();
            try {
                if (sel==null) {
                    sel = cb.getEditor().getText();
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
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Unable to add \""+ selectedProtocolName +"\" protocol.");
                alert.initOwner(rootPane.getScene().getWindow());

                alert.showAndWait();
            }
        };

        cb.setOnKeyReleased( e -> {
            if (ENTER.equals(e.getCode())) {
                onAppendLayer.accept(null);
            }
        });

        addBtn.setOnAction( e-> onAppendLayer.accept(null) );

        HBox controls = new HBox(10);
        controls.getChildren().add(cb);
        controls.getChildren().add(addBtn);
        
        TitledPane titledPane = new TitledPane();
        titledPane.setPadding(new Insets(10,0,0,0));
        titledPane.setCollapsible(false);
        titledPane.setId("append-protocol-pane");
        titledPane.setText("Append new layer");
        titledPane.setContent(controls);
        return titledPane;
    }
    
    protected PacketEditorModel getModel() {
        return controller.getModel();
    }

    private Node getFieldLabel(CombinedField field) {
        HBox row = new HBox();
        Label lblInfo = new Label();
        Label lblName = new Label(field.getMeta().getName());

        FieldData scapyData = field.getScapyFieldData();

        if (scapyData != null && scapyData.hasPosition()) {
            int protocolOffset = field.getProtocol().getScapyProtocol().offset.intValue();
            int len = scapyData.getLength();
            int begin = protocolOffset + scapyData.getOffset();
            int end = begin + Math.max(len - 1, 0);

            if (len > 0) {
                lblInfo.setText(String.format("%04x-%04x [%04d]", begin, end, len));
            } else {
                lblInfo.setText(String.format("%04x-%04x [bits]", begin, end));
            }
        } else {
            lblInfo.setText("meta-field");
        }

        lblInfo.setOnMouseClicked(e -> controller.selectField(field));
        lblName.setOnMouseClicked(e -> controller.selectField(field));
        lblName.setTooltip(new Tooltip(field.getMeta().getId()));
        lblName.setId(getUniqueIdFor(field) + "-label");

        if (scapyData != null && scapyData.isIgnored()) {
            lblInfo.getStyleClass().add("ignored-field");
            lblInfo.setText("ignored");
        }

        lblInfo.getStyleClass().add("field-label-info");
        lblName.getStyleClass().add("field-label-name");
        row.getChildren().add(lblInfo);
        row.getChildren().add(lblName);
        return row;
    }

    private Node buildIndentedFieldLabel(String info, String name, Boolean isBitFlag) {
        HBox row = new HBox();
        Label lblInfo = new Label(info);
        Label lblName = new Label(name);

        lblInfo.getStyleClass().add("field-label-info");
        if (isBitFlag) {
            lblName.getStyleClass().add("bitflag-label-name");
        } else {
            lblName.getStyleClass().add("field-label-name");
        }
        lblName.getStyleClass().add("indented");
        row.getChildren().add(lblInfo);
        row.getChildren().add(lblName);
        return row;
    }

    private Node buildIndentedFieldLabel(String info, String name) {
        return buildIndentedFieldLabel(info, name, false);
    }

    public String getUniqueIdFor(CombinedField field) {
        List<String> fullpath = new ArrayList<>(field.getProtocol().getPath());
        fullpath.add(field.getMeta().getId());
        return fullpath.stream().collect(Collectors.joining("-"));
    }

    protected Node buildFieldRow(CombinedField field) {
        FieldMetadata meta = field.getMeta();
        FieldType type = meta.getType();

        HBox row = new HBox();
        row.setOnMouseClicked(e -> setSelectedRow(row));

        String even = "-even";
        if (!BYTES.equals(type) && oddIndex % 2 != 0) {
            even = "-odd";
        }
        row.getStyleClass().addAll("field-row" + even);
        oddIndex++;

        BorderPane titlePane = new BorderPane();
        titlePane.setLeft(getFieldLabel(field));
        titlePane.getStyleClass().add("title-pane");

        ProtocolField fieldControl = injector.getInstance(ProtocolField.class);
        fieldControl.init(field);

        BorderPane valuePane = new BorderPane();
        valuePane.setCenter(fieldControl);
        row.getChildren().addAll(titlePane, valuePane);
        row.setOnContextMenuRequested(e -> {
            ContextMenu contextMenu = fieldControl.getContextMenu();
            if (contextMenu != null) {
                e.consume();
                contextMenu.show(row, e.getScreenX(), e.getScreenY());
            }
        });
        return row;
    }

    private List<Node> createTCPOptionRows(CombinedField field) {
        return TCPOptionsData.fromFieldData(field.getScapyFieldData()).stream().map(fd -> {
            BorderPane titlePane = new BorderPane();
            titlePane.setLeft(buildIndentedFieldLabel("", fd.getName()));
            titlePane.getStyleClass().add("title-pane");
            titlePane.setOnMouseClicked(e -> controller.selectField(field));

            HBox row = new HBox();
            row.setOnMouseClicked(e -> setSelectedRow(row));
            row.getStyleClass().addAll("field-row");

            BorderPane valuePane = new BorderPane();
            Text valueCtrl = new Text();
            if (fd.hasValue()) {
                valueCtrl.setText(fd.getDisplayValue());
            } else {
                valueCtrl.setText("-");
            }
            valuePane.setLeft(valueCtrl);
            row.getChildren().addAll(titlePane, valuePane);
            return row;
        }).collect(Collectors.toList());
    }

    private String maskToString(int mask) {
        return String.format("%8s", Integer.toBinaryString(mask)).replace(' ', '.').replace('0', '.');
    }

    private List<Node> createBitFlagRows(CombinedField field) {
        return field.getMeta().getBits().stream().map(bitFlagMetadata -> {
            String flagName = bitFlagMetadata.getName();
            int flagMask = bitFlagMetadata.getMask();
            Node label = buildIndentedFieldLabel(maskToString(flagMask), flagName, true);
            ComboBox<ComboBoxItem> combo = createBitFlagComboBox(field, bitFlagMetadata, flagName, flagMask);
            return createRow(label, combo, field);
        }).collect(Collectors.toList());
    }

    private Node createRow(Node label, Node control, CombinedField field) {
        BorderPane titlePane = new BorderPane();
        
        titlePane.setLeft(label);
        titlePane.getStyleClass().add("title-pane");
        if (field != null) {
            titlePane.setOnMouseClicked(e -> controller.selectField(field));
        }

        HBox row = new HBox();
        row.setOnMouseClicked(e -> setSelectedRow(row));
        row.getStyleClass().addAll("field-row-flags");
        if (oddIndex %2 == 0) oddIndex++;

        BorderPane valuePane = new BorderPane();
        valuePane.setLeft(control);
        row.getChildren().addAll(titlePane, valuePane);
        return row;
    }
    
    private ComboBox<ComboBoxItem> createBitFlagComboBox(CombinedField field, BitFlagMetadata bitFlagMetadata, String flagName, int flagMask) {
        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.setId(getUniqueIdFor(field));
        combo.getStyleClass().setAll("control", "bitflag");

        List<ComboBoxItem> items = bitFlagMetadata.getValues().entrySet().stream()
                .map(entry -> new ComboBoxItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        combo.getItems().addAll(items);

        combo.setId(getUniqueIdFor(field) + "-" + flagName);

        ComboBoxItem defaultValue = null;

        if (field.getValue() instanceof JsonPrimitive) {
            Integer fieldValue = field.getValue().getAsInt();
            defaultValue = items.stream().filter(item ->
                    (fieldValue & flagMask) == item.getValue().getAsInt()
            ).findFirst().orElse(null);
        }

        combo.setValue(defaultValue);

        combo.setOnAction((event) -> {
            ComboBoxItem val = combo.getSelectionModel().getSelectedItem();
            int bitFlagMask = bitFlagMetadata.getMask();
            int selected = val.getValue().getAsInt();
            int current = field.getValue().getAsInt();
            String newVal = String.valueOf(current & ~(bitFlagMask) | selected);
            getModel().editField(field, newVal);
        });
        return combo;
    }

    public void displayConnectionError() {
        ConnectionErrorDialog dialog = new ConnectionErrorDialog();
        dialog.showAndWait();
    }

    public void showEmptyPacketContent() {
        BorderPane emptyPacketPane = new BorderPane();
        emptyPacketPane.setCenter(new Label("Empty packet. Please click to add default protocol."));
        
        emptyPacketPane.setOnMouseClicked((event) -> controller.newPacket());
        
        rootPane.getChildren().add(emptyPacketPane);
    }

    public void reset() {
        rootPane.getChildren().clear();
        showEmptyPacketContent();
    }
}
