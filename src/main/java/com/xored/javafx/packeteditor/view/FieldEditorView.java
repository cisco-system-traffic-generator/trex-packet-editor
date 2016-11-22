package com.xored.javafx.packeteditor.view;

import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.controls.ProtocolField;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocol;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocolModel;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import com.xored.javafx.packeteditor.scapy.TCPOptionsData;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.*;

public class FieldEditorView {
    @Inject
    protected FieldEditorController controller;

    protected StackPane rootPane;

    protected Logger logger = LoggerFactory.getLogger(FieldEditorView.class);

    @Inject
    @Named("resources")
    protected ResourceBundle resourceBundle;

    @Inject
    protected Injector injector;

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

    public void setRootPane(StackPane rootPane) {
        this.rootPane = rootPane;
    }

    private MenuItem addMenuItem(ContextMenu ctxMenu, String text, EventHandler<ActionEvent> action) {
        MenuItem menuItem = new MenuItem();
        menuItem.setText(text);
        menuItem.setOnAction(action);
        ctxMenu.getItems().add(menuItem);
        return menuItem;
    }

    public TitledPane buildLayer(CombinedProtocol protocol) {
        TitledPane layerPane = new TitledPane();
        layerPane.setId(getLayerId(protocol) + "-pane");
        
        layerPane.getStyleClass().add(getLayerStyleClass(protocol));
        
        GridPane layerContent = new GridPane();
        layerContent.getStyleClass().add("protocolgrid");

        int layerRowIdx = 0;

        oddIndex = 0;
        for(Node row : buildLayerRows(protocol)) {
            layerContent.add(row, 0, layerRowIdx++);
        }

        String layerTitle = getLayerTitle(protocol);
        layerPane.setText(layerTitle);
        layerPane.setContent(layerContent);
        configureLayerExpandCollapse(layerPane, protocol);
        layerPane.setContextMenu(getLayerContextMenu(protocol));
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
    
    protected void configureLayerExpandCollapse(TitledPane layerPane, CombinedProtocol protocol) {
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
        FieldEditorModel model = controller.getModel();
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
            return "invalid-protocol";
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
    
    public TitledPane buildFETitlePane(String title, Node content) {
        TitledPane pane = new TitledPane();
        pane.setText(title);
        pane.getStyleClass().add("append-protocol");
        pane.setId("fe-parameters-pane");
        pane.setContent(content);
        pane.setCollapsible(false);
        return pane;
    }

    public void rebuild(CombinedProtocolModel model) {
        try {
            protocolTitledPanes = new ArrayList<>();

            model.getProtocolStack().stream().forEach(proto -> {
                protocolTitledPanes.add(buildLayer(proto));
            });

            VBox protocolsPaneVbox = new VBox();
            protocolsPaneVbox.getChildren().setAll(protocolTitledPanes);
            rootPane.getChildren().setAll(protocolsPaneVbox);
        } catch(Exception e) {
            logger.error("Error occurred during rebuilding view. Error {}", e);
        }
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
            controller.getModel().editField(field, newVal);
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
