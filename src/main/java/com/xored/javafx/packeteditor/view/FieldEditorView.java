package com.xored.javafx.packeteditor.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.data.ScapyField;
import com.xored.javafx.packeteditor.data.IField.Type;
import com.xored.javafx.packeteditor.data.ScapyProtocol;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocol;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocolModel;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.scapy.ScapyDefinitions;
import com.xored.javafx.packeteditor.scapy.TCPOptionsData;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.data.IField.Type.*;

public class FieldEditorView {
    @Inject
    FieldEditorController controller;

    private StackPane fieldEditorPane;
    
    private VBox protocolsPane = new VBox();
    
    private Logger logger = LoggerFactory.getLogger(FieldEditorView.class);

    @Inject
    @Named("resources")
    ResourceBundle resourceBundle;
    
    public void setParentPane(StackPane parentPane) {
        this.fieldEditorPane = parentPane;
    }

    public TitledPane buildProtocolPane(CombinedProtocol protocol) {

        TitledPane gridTitlePane = new TitledPane();
        
        GridPane grid = new GridPane();
        grid.getStyleClass().add("protocolgrid");
        grid.setVgap(4);
        grid.setPadding(new Insets(5, 5, 5, 5));

        final int[] ij = {0, 0}; // col, row

        protocol.getFields().stream().forEach(field -> {
            List<Node> list = buildFieldRow(field);
            FieldMetadata meta = field.getMeta();
            Type type = meta.getType();

            for (Node n: list) {
                grid.add(n, ij[0]++, ij[1], 1, 1);
                if(BITMASK.equals(type)) {
                    ij[0] = 0;
                    ij[1]++;
                }
                else if(TCP_OPTIONS.equals(type)) {
                    ij[0] = 0;
                    ij[1]++;
                }
            }
            ij[0] = 0;
            ij[1]++;
        });
        gridTitlePane.setText(protocol.getMeta().getName());
        gridTitlePane.setContent(grid);

        return gridTitlePane;
    }

    public TitledPane buildAppendProtocolPane() {
        TitledPane pane = new TitledPane();
        pane.setText("Append layer");
        pane.getStyleClass().add("append-protocol");
        HBox controls = new HBox();
        pane.setContent(controls);

        List<ProtocolMetadata> protocols = controller.getAvailbleProtocolsToAdd();
        if (protocols.isEmpty()) {
            pane.setExpanded(false);
        }
        ComboBox cb = new ComboBox();
        cb.getStyleClass().add("layer-type-selector");
        cb.setEditable(true);
        cb.getItems().addAll(protocols);

        // Display only available protocols, but let user choose any
        List<String> protoIds = controller.getMetadataService().getProtocols().values().stream()
                .map(m -> m.getId()).sorted()
                .collect(Collectors.toList());

        TextFields.bindAutoCompletion(cb.getEditor(), protoIds);

        Button addBtn = new Button();
        addBtn.setText("Add");
        addBtn.setOnAction(e->{
            Object sel = cb.getSelectionModel().getSelectedItem();
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
        });

        controls.getChildren().add(cb);
        controls.getChildren().add(addBtn);
        controls.setHgrow(cb, Priority.ALWAYS);
        return pane;
    }

    public void rebuild(CombinedProtocolModel model) {
        try {
            fieldEditorPane.getChildren().clear();
            protocolsPane.getChildren().clear();
            model.getProtocolStack().stream().forEach(proto ->
                protocolsPane.getChildren().add(buildProtocolPane(proto))
            );
            protocolsPane.getChildren().add(buildAppendProtocolPane());
            fieldEditorPane.getChildren().add(protocolsPane);
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
            int end = begin + len;

            if (len > 0) {
                lblInfo.setText(String.format("%04d-%04d [%04d]", begin, end, len));
            } else {
                lblInfo.setText(String.format("%04d-%04d [bits]", begin, end));
            }
        } else {
            lblInfo.setText("meta-field");
        }

        lblInfo.setOnMouseClicked(e-> controller.selectField(field));
        lblName.setOnMouseClicked(e-> controller.selectField(field));
        lblName.setTooltip(new Tooltip(field.getMeta().getId()));

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

    private Node buildIndentedFieldLabel(String info, String name) {
        HBox row = new HBox();
        Label lblInfo = new Label(info);
        Label lblName = new Label(name);

        lblInfo.getStyleClass().add("field-label-info");
        lblName.getStyleClass().add("field-label-name");
        lblName.getStyleClass().add("indented");
        row.getChildren().add(lblInfo);
        row.getChildren().add(lblName);
        return row;
    }

    private String getUniqueIdFor(CombinedField field) {
        List<String> fullpath = new ArrayList<>(field.getProtocol().getPath());
        fullpath.add(field.getMeta().getId());
        return fullpath.stream().collect(Collectors.joining("-"));
    }

    private List<Node> buildFieldRow(CombinedField field) {
        List<Node> rows = new ArrayList<>();
        FieldMetadata meta = field.getMeta();
        Type type = meta.getType();

        HBox row = new HBox();
        row.getStyleClass().addAll("field-row");

        BorderPane titlePane = new BorderPane();
        titlePane.setLeft(getFieldLabel(field));
        titlePane.getStyleClass().add("title-pane");

        if(BITMASK.equals(type)) {
            row.getChildren().add(titlePane);
            rows.add(row);
            field.getMeta().getBits().stream().forEach(bitFlagMetadata -> rows.add(this.createBitFlagRow(field, bitFlagMetadata)));
        } else {
            Control fieldControl = createDefaultControl(field);
            
            fieldControl.setId(getUniqueIdFor(field));
            fieldControl.getStyleClass().addAll("control");
            
            BorderPane valuePane = new BorderPane();
            valuePane.setCenter(fieldControl);
            if (RAW.equals(type)) {
                valuePane.getStyleClass().addAll("field-row-raw-value");
            }
            row.getChildren().addAll(titlePane, valuePane);
            rows.add(row);
            // TODO: remove this crutch :)
            if(TCP_OPTIONS.equals(type) && field.getScapyFieldData() != null) {
                TCPOptionsData.fromFieldData(field.getScapyFieldData()).stream().forEach(fd ->
                        rows.add(createTCPOptionRow(fd))
                );
            }
        }

        return rows;
    }

    private Control createDefaultControl(CombinedField field) {
        String humanVal = field.getDisplayValue();
        String labelText = humanVal;

        boolean isDefaultValue = !controller.getModel().isBinaryMode() && !field.hasUserValue();

        if (field.getMeta().getType() == Type.ENUM) {
            // for enums also show value
            JsonElement val = field.getMeta().getDictionary().getOrDefault(humanVal, null);
            if (val != null) {
                labelText = String.format("%s (%s)", humanVal, val.toString());
            }
        } else if (isDefaultValue && field.getMeta().isAuto()) {
            labelText = String.format("%s (auto-calculated)", humanVal);
        }

        Label label = new Label(labelText);

        if (isDefaultValue) {
            label.getStyleClass().add("field-value-default");
        }

        label.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            Control editableControl = createControl(field, label);
            label.setGraphic(editableControl);
            editableControl.requestFocus();
        });
        return label;
    }
    
    private Control createControl(CombinedField field, Label parent) {
        Control fieldControl;
        switch(field.getMeta().getType()) {
            case ENUM:
                fieldControl = createEnumField(field, parent);
                break;
            case RAW:
                FieldData fieldData = field.getScapyFieldData();
                if (fieldData != null && fieldData.hasBinaryData() && !(fieldData.getValue() instanceof JsonPrimitive)) {
                    fieldControl = new Label(field.getDisplayValue());
                } else {
                    TextArea ta = new TextArea(field.getDisplayValue());
                    ta.setPrefSize(200, 40);
                    MenuItem saveRawMenuItem = new MenuItem(resourceBundle.getString("SAVE_PAYLOAD_TITLE"));
                    saveRawMenuItem.setOnAction((event) -> controller.getModel().editField(field, ta.getText()));
                    ta.setContextMenu(new ContextMenu(saveRawMenuItem));
                    fieldControl = ta;
                }
                fieldControl.getStyleClass().addAll("field-row-raw-value");
                break;
            case NONE:
            default:
                fieldControl = createTextField(field, parent);
        }
        
        return fieldControl;
    }
    
    private TextField createTextField(CombinedField field, Label parent) {
        TextField tf;
        switch(field.getMeta().getType()) {
            case MAC_ADDRESS:
            case IPV4ADDRESS:
            case TCP_OPTIONS:
            case NUMBER:
            case STRING:
                tf = createFieldTextProperty(field, parent);
                break;
            default:
                return null;
        }
        addOnclickListener(tf, field, parent);
        injectOnChangeHandler(tf, field, parent);
        tf.setContextMenu(getContextMenu(field));
        return  tf;
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

    private TextField createFieldTextProperty(CombinedField field, Label parent) {
        CustomTextField tf = (CustomTextField)TextFields.createClearableTextField();
        tf.rightProperty().get().setOnMouseReleased(event ->
                clearFieldValue(field)
        );

        tf.setText(field.getValue().getAsString());
        return tf;
    }

    private String maskToString(int mask) {
        return String.format("%8s", Integer.toBinaryString(mask)).replace(' ', '.').replace('0', '.');
    }

    private Node createBitFlagRow(CombinedField field, BitFlagMetadata bitFlagMetadata) {
        BorderPane titlePane = new BorderPane();
        String flagName = bitFlagMetadata.getName();
        int flagMask = bitFlagMetadata.getMask();

        titlePane.setLeft(buildIndentedFieldLabel(maskToString(flagMask), flagName));
        titlePane.getStyleClass().add("title-pane");
        HBox row = new HBox();
        row.getStyleClass().addAll("field-row");


        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.getStyleClass().addAll("control");
        
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
    
    private void addOnclickListener(Node node, CombinedField field, Label parent) {
        node.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            controller.selectField(field);
            parent.setGraphic(node);
        });
    }

    private void injectOnChangeHandler(TextField textField, CombinedField field, Label parent) {
        textField.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                parent.setGraphic(null);
                controller.getModel().editField(field, ReconstructField.setHumanValue(field.getId(), textField.getText()));
            }
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                parent.setGraphic(null);
            }
        });
    }

    private Control createEnumField(CombinedField field, Label parent) {
        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.getStyleClass().addAll("control");
        List<ComboBoxItem> items = field.getMeta().getDictionary().entrySet().stream()
                .sorted((e1, e2)->e1.getKey().compareTo(e2.getKey()))
                .map(entry -> new ComboBoxItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        
        ComboBoxItem defaultValue = items.stream().filter(item ->
                item.equalsTo(field.getValue())
        ).findFirst().orElse(null);

        if (defaultValue == null && field.getScapyFieldData() != null) {
            String label = field.getValue().toString();
            FieldData fd = field.getScapyFieldData();
            defaultValue = new ComboBoxItem(fd.getHumanValue(), fd.value);
            items.add(defaultValue);
        }
        combo.getItems().addAll(items);
        if (defaultValue != null) {
            combo.setValue(defaultValue);
        }

        TextFields.bindAutoCompletion(combo.getEditor(), items.stream().map(f -> f.toString()).collect(Collectors.toList()));
        combo.setOnAction((event) -> {
            ComboBoxItem val = null;
            Object sel = combo.getSelectionModel().getSelectedItem(); // yes, it can be string
            if (sel instanceof String) {
                val = items.stream().filter(f -> f.toString().equals(sel)).findFirst().orElse(null);
            } else if (sel instanceof ComboBoxItem) {
                val = (ComboBoxItem)sel;
            }
            if (val != null) {
                controller.getModel().editField(field, ReconstructField.setValue(field.getId(), val.getValue().getAsString()));
            }
        });

        return combo;
    }

    private void clearFieldValue(CombinedField field) {
        controller.getModel().editField(field, ReconstructField.resetValue(field.getMeta().getId()));
    }

    private void randomizeFieldValue(CombinedField field) {
        controller.getModel().editField(field, ReconstructField.randomizeValue(field.getMeta().getId()));
    }

    private ContextMenu getContextMenu(CombinedField field) {
        ContextMenu context = new ContextMenu();

        MenuItem generateItem = new MenuItem(resourceBundle.getString("GENERATE"));
        generateItem.setOnAction(event ->
                clearFieldValue(field)
        );

        MenuItem defaultItem = new MenuItem(resourceBundle.getString("SET_DEFAULT"));
        defaultItem.setOnAction(event ->
                randomizeFieldValue(field)
        );

        context.getItems().addAll(generateItem, defaultItem);
        
        return context;
    }
}
