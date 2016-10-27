package com.xored.javafx.packeteditor.view;

import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.controls.ProtocolField;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocol;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocolModel;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import com.xored.javafx.packeteditor.scapy.TCPOptionsData;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.*;

public class FieldEditorView {
    @Inject
    FieldEditorController controller;

    private StackPane fieldEditorPane;
    
    private Logger logger = LoggerFactory.getLogger(FieldEditorView.class);

    @Inject
    @Named("resources")
    ResourceBundle resourceBundle;

    @Inject
    Injector injector;

    private AutoCompletionBinding<String> protoAutoCompleter;

    static private int oddIndex = 0;

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

    public void setParentPane(StackPane parentPane) {
        this.fieldEditorPane = parentPane;
    }


    public TitledPane buildProtocolPane(CombinedProtocol protocol) {
        TitledPane gridTitlePane = new TitledPane();
        String protocolPathId = protocol.getPath().stream().collect(Collectors.joining("-"));
        gridTitlePane.setId(protocolPathId + "-pane");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("protocolgrid");

        final int[] ij = {0, 0}; // col, row

        oddIndex = 0;
        protocol.getFields().stream().forEach(field -> {
            FieldMetadata meta = field.getMeta();
            FieldMetadata.FieldType type = meta.getType();
            List<Node> list;

            list = buildFieldRow(field);

            for (Node n : list) {
                grid.add(n, ij[0]++, ij[1], 1, 1);
                if (BITMASK.equals(type)
                        || TCP_OPTIONS.equals(type)
                        || BYTES.equals(type)) {
                    ij[0] = 0;
                    ij[1]++;
                }
            }
            ij[0] = 0;
            ij[1]++;
        });
        String title = protocol.getMeta().getName();
        UserProtocol userProtocol = protocol.getUserProtocol();
        ProtocolData protocolData = protocol.getScapyProtocol();
        String subtype = null;
        if (userProtocol != null && protocolData == null) {
            subtype = "as Payload";
            gridTitlePane.getStyleClass().add("invalid-protocol");
        } else if (userProtocol != null && protocolData != null && protocolData.isInvalidStructure()) {
            if (protocolData.getRealId() == null) {
                subtype = "as Payload";
            } else {
                subtype = "as " + protocolData.getRealId();
            }
            gridTitlePane.getStyleClass().add("invalid-protocol");
        } else if (userProtocol != null && protocolData != null && protocolData.protocolRealIdDifferent() ) {
            subtype = "as " + protocolData.getRealId();
        }

        if (subtype != null) {
            title = title + " " + subtype;
        }

        gridTitlePane.setText(title);
        gridTitlePane.setContent(grid);

        if (userProtocol != null) {
            gridTitlePane.setExpanded(!userProtocol.isCollapsed());
            gridTitlePane.expandedProperty().addListener(val->
                    userProtocol.setCollapsed(!gridTitlePane.isExpanded())
            );
        }

        return gridTitlePane;
    }

    public TitledPane buildAppendProtocolPane() {
        TitledPane pane = new TitledPane();
        pane.setText("Append new layer");
        pane.getStyleClass().add("append-protocol");
        HBox controls = new HBox(10);
        pane.setId("append-protocol-pane");
        pane.setContent(controls);

        List<ProtocolMetadata> protocols = controller.getModel().getAvailableProtocolsToAdd(false);
        if (protocols.isEmpty()) {
            pane.setExpanded(false);
        }
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
                if (sel instanceof ProtocolMetadata) {
                    controller.getModel().addProtocol((ProtocolMetadata)sel);
                } else if (sel instanceof String) {
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
                alert.initOwner(fieldEditorPane.getScene().getWindow());
                
                alert.showAndWait();
            }
        };

        cb.setOnKeyReleased( e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                onAppendLayer.accept(null);
            }
        });

        addBtn.setOnAction( e-> onAppendLayer.accept(null) );

        controls.getChildren().add(cb);
        controls.getChildren().add(addBtn);
        return pane;
    }

    public void rebuild(CombinedProtocolModel model) {
        try {
            VBox protocolsPane = new VBox();

            model.getProtocolStack().stream().forEach(proto -> {
                protocolsPane.getChildren().add(buildProtocolPane(proto));
            });
            protocolsPane.getChildren().add(buildAppendProtocolPane());

            fieldEditorPane.getChildren().setAll(protocolsPane);
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

    private Node getEmptyFieldLabel() {
        HBox row = new HBox();
        Label lblInfo = new Label("");
        Label lblName = new Label("");

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

    private List<Node> buildFieldRow(CombinedField field) {
        List<Node> rows = new ArrayList<>();
        FieldMetadata meta = field.getMeta();
        FieldMetadata.FieldType type = meta.getType();

        HBox row = new HBox();

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
        rows.add(row);
        if(BITMASK.equals(type)) {
            field.getMeta().getBits().stream().forEach(bitFlagMetadata -> rows.add(this.createBitFlagRow(field, bitFlagMetadata)));
        }
        // TODO: remove this crutch :)
        if(TCP_OPTIONS.equals(type) && field.getScapyFieldData() != null) {
            TCPOptionsData.fromFieldData(field.getScapyFieldData()).stream().forEach(fd ->
                            rows.add(createTCPOptionRow(fd))
            );
        }

        return rows;
    }


    private Node createTCPOptionRow(TCPOptionsData tcpOption) {
        // TODO: reuse code
        BorderPane titlePane = new BorderPane();
        titlePane.setLeft(buildIndentedFieldLabel("", tcpOption.getName()));
        titlePane.getStyleClass().add("title-pane");
        HBox row = new HBox();
        row.getStyleClass().addAll("field-row");


        BorderPane valuePane = new BorderPane();
        Text valueCtrl = new Text();
        if (tcpOption.hasValue()) {
            valueCtrl.setText(tcpOption.getDisplayValue());
        } else {
            valueCtrl.setText("-");
        }
        valuePane.setLeft(valueCtrl);
        row.getChildren().addAll(titlePane, valuePane);
        return row;
    }

    private String maskToString(int mask) {
        return String.format("%8s", Integer.toBinaryString(mask)).replace(' ', '.').replace('0', '.');
    }

    private Node createBitFlagRow(CombinedField field, BitFlagMetadata bitFlagMetadata) {
        BorderPane titlePane = new BorderPane();
        String flagName = bitFlagMetadata.getName();
        int flagMask = bitFlagMetadata.getMask();

        titlePane.setLeft(buildIndentedFieldLabel(maskToString(flagMask), flagName, true));
        titlePane.getStyleClass().add("title-pane");

        HBox row = new HBox();
        row.getStyleClass().addAll("field-row-flags");
        if (oddIndex %2 == 0) oddIndex++;

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
        BorderPane valuePane = new BorderPane();
        valuePane.setLeft(combo);
        row.getChildren().addAll(titlePane, valuePane);
        return row;
    }

    public void displayConnectionError() {
        ConnectionErrorDialog dialog = new ConnectionErrorDialog();
        dialog.showAndWait();
    }

    public void showEmptyPacketContent() {
        BorderPane emptyPacketPane = new BorderPane();
        emptyPacketPane.setCenter(new Label("Empty packet. Please click to add default protocol."));
        
        emptyPacketPane.setOnMouseClicked((event) -> controller.newPacket());
        
        fieldEditorPane.getChildren().add(emptyPacketPane);
    }

    public void reset() {
        fieldEditorPane.getChildren().clear();
        showEmptyPacketContent();
    }
}
