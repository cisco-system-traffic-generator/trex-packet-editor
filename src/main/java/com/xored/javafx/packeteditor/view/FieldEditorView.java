package com.xored.javafx.packeteditor.view;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.data.Field;
import com.xored.javafx.packeteditor.data.IField.Type;
import com.xored.javafx.packeteditor.data.Protocol;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.scapy.TCPOptionsData;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import jidefx.scene.control.field.MaskTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.data.IField.Type.BITMASK;
import static com.xored.javafx.packeteditor.data.IField.Type.RAW;
import static com.xored.javafx.packeteditor.data.IField.Type.TCP_OPTIONS;

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

    public TitledPane buildProtocolPane(Protocol protocol) {

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
        gridTitlePane.setText(protocol.getName());
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
        if (!protocols.isEmpty()) {
            cb.getSelectionModel().select(0);
        }
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

    public void rebuild(Stack<Protocol> protocols) {
        try {
            fieldEditorPane.getChildren().clear();
            protocolsPane.getChildren().clear();
            protocols.stream().forEach(
                    p -> protocolsPane.getChildren().add(buildProtocolPane(p))
            );
            protocolsPane.getChildren().add(buildAppendProtocolPane());
            fieldEditorPane.getChildren().add(protocolsPane);
        } catch(Exception e) {
            logger.error("Error occurred during rebuilding view. Error {}", e);
        }
    }

    private Node getFieldLabel(Field field) {
        HBox row = new HBox();
        Label lblInfo = new Label();
        Label lblName = new Label(field.getName());

        if (field.getData().hasPosition()) {
            int len = field.getLength();
            int begin = field.getAbsOffset();
            int end = begin + len;

            if (len > 0) {
                lblInfo.setText(String.format("%04d-%04d [%04d]", begin, end, len));
            } else {
                lblInfo.setText(String.format("%04d-%04d [bits]", begin, end));
            }
        }

        lblInfo.setOnMouseClicked(e-> controller.selectField(field));
        lblName.setOnMouseClicked(e-> controller.selectField(field));
        lblName.setTooltip(new Tooltip(field.getId()));

        if (field.getData().isIgnored()) {
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

    private List<Node> buildFieldRow(Field field) {
        List<Node> rows = new ArrayList<>();
        String title = field.getName();
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
            
            fieldControl.setId(field.getUniqueId());
            fieldControl.getStyleClass().addAll("control");
            
            BorderPane valuePane = new BorderPane();
            valuePane.setCenter(fieldControl);
            if (RAW.equals(type)) {
                valuePane.getStyleClass().addAll("field-row-raw-value");
            }
            row.getChildren().addAll(titlePane, valuePane);
            rows.add(row);
            // TODO: remove this crutch :)
            if(TCP_OPTIONS.equals(type)) {
                TCPOptionsData.fromFieldData(field.getData()).stream().forEach(fd -> rows.add(createTCPOptionRow(fd)));
            }
        }

        return rows;
    }

    private Control createDefaultControl(Field field) {
        Label label = new Label(field.getDisplayValue());
        label.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            Control editableControl = createControl(field, label);
            label.setGraphic(editableControl);
            editableControl.requestFocus();
        });
        return label;
    }
    
    private Control createControl(Field field, Label parent) {
        Control fieldControl;
        switch(field.getType()) {
            case ENUM:
                fieldControl = createEnumField(field, parent);
                break;
            case RAW:
                if (field.getData().hasBinaryData() && !field.getData().hasValue()) {
                    fieldControl = new Label(field.getDisplayValue());
                } else {
                    TextArea ta = new TextArea(field.getData().hvalue);
                    ta.setPrefSize(200, 40);
                    MenuItem saveRawMenuItem = new MenuItem(resourceBundle.getString("SAVE_PAYLOAD_TITLE"));
                    saveRawMenuItem.setOnAction((event) -> field.setStringValue(ta.getText()));
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
    
    private TextField createTextField(Field field, Label parent) {
        TextField tf;
        switch(field.getType()) {
            case MAC_ADDRESS:
                tf = createMacAddressField(field, parent);
                break;
            case IPV4ADDRESS:
                tf = createIPAddressField(field, parent);
                break;
            case TCP_OPTIONS:
            case NUMBER:
            case STRING:
                tf = new TextField(field.getDisplayValue());
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

    private TextField createMacAddressField(Field field, Label parent) {
        MaskTextField macField = MaskTextField.createMacAddressField();
        macField.setText(field.getValue().getAsString());
        return macField;
    }

    private TextField createIPAddressField(Field field, Label parent) {
        TextField textField = new TextField();
        textField.setText(field.getValue().getAsString());
        return textField;
    }

    private String maskToString(int mask) {
        return String.format("%8s", Integer.toBinaryString(mask)).replace(' ', '.').replace('0', '.');
    }

    private Node createBitFlagRow(Field field, BitFlagMetadata bitFlagMetadata) {
        BorderPane titlePane = new BorderPane();
        String flagName = bitFlagMetadata.getName();
        String maskString = maskToString(bitFlagMetadata.getMask());

        titlePane.setLeft(buildIndentedFieldLabel(maskString, flagName));
        titlePane.getStyleClass().add("title-pane");
        HBox row = new HBox();
        row.getStyleClass().addAll("field-row");


        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.getStyleClass().addAll("control");
        
        List<ComboBoxItem> items = bitFlagMetadata.getValues().entrySet().stream()
                .map(entry -> new ComboBoxItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        combo.getItems().addAll(items);

        combo.setId(field.getUniqueId() + "-" + flagName);
        
        Integer bitFlagValue = field.getValue().getAsInt();
        
        ComboBoxItem defaultValue;
        
        Optional<ComboBoxItem> res = items.stream().filter(item -> (bitFlagValue & item.getValue().getAsInt()) > 0).findFirst();
        if(res.isPresent()) {
            defaultValue = res.get();
        } else {
            Optional<ComboBoxItem> unsetValue = items.stream().filter(item -> (item.getValue().getAsInt() == 0)).findFirst();
            defaultValue = unsetValue.isPresent()? unsetValue.get() : null;
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
    
    private void addOnclickListener(Node node, Field field, Label parent) {
        node.addEventHandler(MouseEvent.MOUSE_CLICKED, (mouseEvent) -> {
            controller.selectField(field);
            parent.setGraphic(node);
        });
    }

    private void injectOnChangeHandler(TextField textField, Field field, Label parent) {
        textField.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                parent.setGraphic(null);
                controller.getModel().editField(field, ReconstructField.setValue(field.getId(), textField.getText()));
            }
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                parent.setGraphic(null);
            }
        });
    }

    private void injectOnChangeHandler(ComboBox<ComboBoxItem> combo, Field field, Label parent) {
        combo.setOnAction((event) -> {
            ComboBoxItem val = combo.getSelectionModel().getSelectedItem();
            controller.getModel().editField(field, ReconstructField.setValue(field.getId(), val.getValue().getAsString()));
        });
    }
    
    private Control createEnumField(Field field, Label parent) {
        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.getStyleClass().addAll("control");
        List<ComboBoxItem> items = field.getMeta().getDictionary().entrySet().stream()
                .sorted((e1, e2)->e1.getKey().compareTo(e2.getKey()))
                .map(entry -> new ComboBoxItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        
        Optional<ComboBoxItem> defaultValue = items.stream().filter(item -> item.equalsTo(field.getValue())).findFirst();
        if (!defaultValue.isPresent()){
            defaultValue = Optional.of(new ComboBoxItem(field.getData().hvalue, field.getData().value));
            items.add(defaultValue.get());
        }
        combo.getItems().addAll(items);
        injectOnChangeHandler(combo, field, parent);
        if (defaultValue.isPresent()) {
            combo.setValue(defaultValue.get());
        }
        return combo;
    }
    
    private ContextMenu getContextMenu(Field field) {
        ContextMenu context = new ContextMenu();

        MenuItem generateItem = new MenuItem(resourceBundle.getString("GENERATE"));
        generateItem.setOnAction(
                event -> controller.getModel().editField(field, ReconstructField.randomizeValue(field.getId()))
        );

        MenuItem defaultItem = new MenuItem(resourceBundle.getString("SET_DEFAULT"));
        defaultItem.setOnAction(
                event -> controller.getModel().editField(field, ReconstructField.resetValue(field.getId()))
        );

        context.getItems().addAll(generateItem, defaultItem);
        
        return context;
    }
}
