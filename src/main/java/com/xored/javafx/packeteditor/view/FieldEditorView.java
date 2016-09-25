package com.xored.javafx.packeteditor.view;

import com.google.inject.Inject;
import com.xored.javafx.packeteditor.controllers.FieldEditorController2;
import com.xored.javafx.packeteditor.data.Field;
import com.xored.javafx.packeteditor.data.IField.Type;
import com.xored.javafx.packeteditor.data.Protocol;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import jidefx.scene.control.field.MaskTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.data.IField.Type.BITMASK;

public class FieldEditorView {
    String lastFocused;
    @Inject
    FieldEditorController2 controller;
    private Pane parentPane;
    private VBox protocolsPane = new VBox();
    private Logger logger = LoggerFactory.getLogger(FieldEditorView.class);
    public void setParentPane(Pane parentPane) {
        this.parentPane = parentPane;
    }

    public void addProtocol(Protocol protocol) {
        
        protocolsPane.getChildren().add(buildProtocolRow(protocol));
        protocol.getFields().stream().forEach(field -> protocolsPane.getChildren().addAll(buildFieldRow(field)));
    }

    public void rebuild(Stack<Protocol> protocols) {
        parentPane.getChildren().clear();
        protocolsPane.getChildren().clear();
        
        protocols.stream().forEach(this::addProtocol);
        
        parentPane.getChildren().add(protocolsPane);
    }
    
    private HBox buildProtocolRow(Protocol protocol) {
        HBox row = new HBox(13);
        row.getStyleClass().addAll("protocol-row");

        Text textName = new Text(protocol.getName());
        textName.getStyleClass().add("protocol-name");

        // TODO: replace * with proper symbol
        Text icon = new Text("*");

        row.getChildren().addAll(textName);

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
                case NUMBER:
                case STRING:
                    TextField tf = new TextField(field.getDisplayValue());
                    injectOnChangeHandler(tf, field);
                    fieldControl = tf;
                    break;
                case NONE:
                default:
                    fieldControl = new Label("");
            }
            fieldControl.getStyleClass().addAll("control");
            
            BorderPane valuePane = new BorderPane();
            valuePane.setCenter(fieldControl);
            row.getChildren().addAll(titlePane, valuePane);
            addFocusListener(fieldControl, field);
            setFocusIfNeeded(fieldControl, field);
            rows.add(row);
        }

        return rows;
    }

    private MaskTextField createMacAddressField(Field field) {
        MaskTextField maskTextField = MaskTextField.createMacAddressField();
        maskTextField.setText(field.getValue().getAsString());
        injectOnChangeHandler(maskTextField, field);
        return maskTextField;
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
        return textField;
    }

    private Node createBitFlagRow(Field field, BitFlagMetadata bitFlagMetadata) {
        BorderPane titlePane = new BorderPane();
        Text titleLabel = new Text("        "+bitFlagMetadata.getName());
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
            field.setStringValue(String.valueOf(current & ~(bitFlagMask) | selected));
        });
        setFocusIfNeeded(combo, field);
        BorderPane valuePane = new BorderPane();
        valuePane.setLeft(combo);
        row.getChildren().addAll(titlePane, valuePane);
        return row;
    }


    private Control createTextMaskField(Field field, String mask) {
        MaskTextField maskTextField = new MaskTextField();
        maskTextField.setInputMask(mask);
        maskTextField.setText(field.getDisplayValue());
        injectOnChangeHandler(maskTextField, field);
        return maskTextField;
    }

    private void setFocusIfNeeded(Control control, Field field) {
        if (field.getUniqueId().equals(lastFocused)) {
            Platform.runLater(control::requestFocus);
        }
    } 
    
    private void addFocusListener(Node node, Field field) {
        node.setFocusTraversable(false);
        node.focusedProperty().addListener((arg0, oldPropertyValue, focused) -> {
            if (focused) {
                controller.selectField(field);
                this.lastFocused = field.getUniqueId();
            }
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
        combo.getItems().addAll(items);
        injectOnChangeHandler(combo, field);
        if (defaultValue.isPresent()) {
            combo.setValue(defaultValue.get());
        }
        return combo;
    }
}
