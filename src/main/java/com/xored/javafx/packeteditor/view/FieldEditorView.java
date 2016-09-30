package com.xored.javafx.packeteditor.view;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.data.Field;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.data.IField.Type;
import com.xored.javafx.packeteditor.data.Protocol;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.scapy.TCPOptionsData;
import com.xored.javafx.packeteditor.service.PacketDataService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import jidefx.scene.control.field.MaskTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.data.IField.Type.BITMASK;
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
        fieldEditorPane.setPadding(new Insets(25, 25, 25, 50));
    }

    public void addProtocol(Protocol protocol) {
        
        protocolsPane.getChildren().add(buildProtocolRow(protocol));
        protocol.getFields().stream().forEach(field -> protocolsPane.getChildren().addAll(buildFieldRow(field)));
    }

    public void rebuild(Stack<Protocol> protocols) {
        try {
            fieldEditorPane.getChildren().clear();
            protocolsPane.getChildren().clear();
            protocols.stream().forEach(this::addProtocol);
            fieldEditorPane.getChildren().add(protocolsPane);
        } catch(Exception e) {
            logger.error("Error occurred during rebuilding view. Error {}", e);
        }
    }
    
    private HBox buildProtocolRow(Protocol protocol) {
        HBox row = new HBox(13);
        row.getStyleClass().addAll("protocol-row");

        Text textName = new Text("    " + protocol.getName());
        textName.getStyleClass().add("protocol-name");

        // TODO: replace * with proper symbol
        Text icon = new Text("*");

        row.getChildren().addAll(textName);

        return row;
    }

    private List<Node> buildFieldRow(Field field) {
        List<Node> rows = new ArrayList<>();
        String title = field.getName();
        if (field.getData().isIgnored()) {
            title = title + "(ignored)";
        }
        FieldMetadata meta = field.getMeta();
        Type type = meta.getType();

        HBox row = new HBox();
        row.getStyleClass().addAll("field-row");

        BorderPane titlePane = new BorderPane();
        Text titleControl = new Text(title);
        titlePane.setLeft(titleControl);
        titlePane.getStyleClass().add("title-pane");

        if(BITMASK.equals(type)) {
            row.getChildren().add(titlePane);
            rows.add(row);
            field.getMeta().getBits().stream().forEach(bitFlagMetadata -> rows.add(this.createBitFlagRow(field, bitFlagMetadata)));
        } else {
            Control fieldControl;
            switch(type) {
                case ENUM:
                    fieldControl = createEnumField(field);
                    break;
                case MAC_ADDRESS:
                    fieldControl = createMacAddressField(field);
                    break;
                case IPV4ADDRESS:
                    fieldControl = createIPAddressField(field);
                    break;
                case TCP_OPTIONS:
                case NUMBER:
                case STRING:
                    TextField tf = new TextField(field.getDisplayValue());
                    injectOnChangeHandler(tf, field);
                    fieldControl = tf;
                    fieldControl.setContextMenu(getContextMenu(field));
                    break;
                case RAW:
                    if (field.getData().hasBinaryData() && !field.getData().hasValue()) {
                        fieldControl = new Label(field.getDisplayValue());
                    } else {
                        row.getStyleClass().addAll("field-row-raw");
                        TextArea ta = new TextArea(field.getData().hvalue);
                        ta.setPrefSize(200, 40);
                        MenuItem saveRawMenuItem = new MenuItem(resourceBundle.getString("SAVE_PAYLOAD_TITLE"));
                        saveRawMenuItem.setOnAction((event) -> field.setStringValue(ta.getText()));
                        ta.setContextMenu(new ContextMenu(saveRawMenuItem));
                        fieldControl = ta;
                    }
                    break;
                case NONE:
                default:
                    fieldControl = new Label("");
            }
            fieldControl.setId(field.getUniqueId());
            fieldControl.getStyleClass().addAll("control");
            
            BorderPane valuePane = new BorderPane();
            valuePane.setCenter(fieldControl);
            row.getChildren().addAll(titlePane, valuePane);
            addOnclickListener(fieldControl, field);
            rows.add(row);
            // TODO: remove this crutch :)
            if(TCP_OPTIONS.equals(type)) {
                TCPOptionsData.fromValue(field.getValue()).stream().forEach(fd -> rows.add(createTCPOptionRow(fd)));
            }
        }

        return rows;
    }

    private Node createTCPOptionRow(TCPOptionsData tcpOption) {
        // TODO: reuse code
        BorderPane titlePane = new BorderPane();
        Text titleLabel = new Text("        "+tcpOption.getName());
        titlePane.setLeft(titleLabel);
        titlePane.getStyleClass().add("title-pane");
        HBox row = new HBox();
        row.getStyleClass().addAll("field-row");


        BorderPane valuePane = new BorderPane();
        if (tcpOption.hasValue()) {
            TextField valueCtrl = new TextField();
            valueCtrl.setText(tcpOption.getDisplayValue());
            valueCtrl.setEditable(false);
            // TODO: implement field editing
            valuePane.setLeft(valueCtrl);
        } else {
            Text valueCtrl = new Text();
            valueCtrl.setText("-");
            valuePane.setLeft(valueCtrl);
        }
        row.getChildren().addAll(titlePane, valuePane);
        return row;
    }

    private MaskTextField createMacAddressField(Field field) {
        MaskTextField macField = MaskTextField.createMacAddressField();
        macField.setText(field.getValue().getAsString());
        injectOnChangeHandler(macField, field);
        macField.setContextMenu(getContextMenu(field));
        return macField;
    }
    private TextField createIPAddressField(Field field) {
        TextField textField = new TextField();
        String partialBlock = "(([01]?[0-9]{0,2})|(2[0-4][0-9])|(25[0-5]))";
        String subsequentPartialBlock = "(\\."+partialBlock+")" ;
        String ipAddress = partialBlock+"?"+subsequentPartialBlock+"{0,3}";
        String regex = "^"+ipAddress;
        final UnaryOperator<TextFormatter.Change> ipAddressFilter = c -> {
            String text = c.getControlNewText();
            if  (text.matches(regex)) {
                return c ;
            } else {
                return null ;
            }
        };
        textField.setTextFormatter(new TextFormatter<>(ipAddressFilter));
        textField.setText(field.getValue().getAsString());
        injectOnChangeHandler(textField, field);
        textField.setContextMenu(getContextMenu(field));
        return textField;
    }

    private Node createBitFlagRow(Field field, BitFlagMetadata bitFlagMetadata) {
        BorderPane titlePane = new BorderPane();
        String flagName = bitFlagMetadata.getName();
        Text titleLabel = new Text("        " + flagName);
        titlePane.setLeft(titleLabel);
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
    
    private void addOnclickListener(Node node, Field field) {
        node.setOnMouseClicked((event) -> {
            controller.selectField(field);
        });
    }
    
    private void injectOnChangeHandler(TextField textField, Field field) {
        textField.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                field.setStringValue(textField.getText());
            }
        });
    }

    private void injectOnChangeHandler(ComboBox<ComboBoxItem> combo, Field field) {
        combo.setOnAction((event) -> {
            ComboBoxItem val = combo.getSelectionModel().getSelectedItem();
            field.setStringValue(val.getValue().getAsString());
        });
    }
    
    private Control createEnumField(Field field) {
        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.getStyleClass().addAll("control");
        List<ComboBoxItem> items = field.getMeta().getDictionary().entrySet().stream().map(entry -> new ComboBoxItem(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        
        Optional<ComboBoxItem> defaultValue = items.stream().filter(item -> item.equalsTo(field.getValue())).findFirst();
        if (!defaultValue.isPresent()){
            defaultValue = Optional.of(new ComboBoxItem(field.getData().hvalue, field.getData().value));
            items.add(defaultValue.get());
        }
        combo.getItems().addAll(items);
        injectOnChangeHandler(combo, field);
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
