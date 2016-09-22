package com.xored.javafx.packeteditor.view;

import com.xored.javafx.packeteditor.data.Field;
import com.xored.javafx.packeteditor.data.IField;
import com.xored.javafx.packeteditor.data.Protocol;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import jidefx.scene.control.field.MaskTextField;

import java.util.List;
import java.util.stream.Collectors;

public class FieldEditorView {
    private Pane parentPane;
    private VBox protocolsPane = new VBox();

    public void setParentPane(Pane parentPane) {
        this.parentPane = parentPane;
    }

    public void addProtocol(Protocol protocol) {
        
        protocolsPane.getChildren().add(buildProtocolRow(protocol));
        protocol.getFields().stream().forEach(field -> protocolsPane.getChildren().add(buildFieldRow(field)));
        rebuild();
    }

    public void rebuild() {
        parentPane.getChildren().clear();
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

    private HBox buildFieldRow(Field field) {
        String title = field.getName();
        FieldMetadata meta = field.getMeta();
        
        HBox row = new HBox(13);
        row.getStyleClass().addAll("field-row");

        BorderPane titlePane = new BorderPane();
        Text titleControl = new Text(title);
        titlePane.setLeft(titleControl);
        titlePane.getStyleClass().add("title-pane");


        IField.Type type = meta.getType();
        Pane valuePane = new Pane();
        switch(type) {
            case ENUM:
                valuePane = createEnumField(field);
                break;
            case MAC_ADDRESS:
                valuePane = createMacAddresField(field);
                break;
            case IPV4ADDRESS:
                valuePane = createIPAddresField(field);
                break;
            case NONE:
            default:

        }
//
//        Hyperlink link = new Hyperlink(value);
//        valuePane.getChildren().add(new ProtocolField(value));
//        valuePane.getStyleClass().add("value-pane");
//
        row.getChildren().addAll(titlePane, valuePane);

        return row;
    }

    private Pane createIPAddresField(Field field) {
        BorderPane pane = new BorderPane();
        MaskTextField ipAddress = new MaskTextField();
        ipAddress.setInputMask("255.255.255.255");
        pane.setCenter(ipAddress);
        return pane;
    }

    private Pane createEnumField(Field field) {
        BorderPane pane = new BorderPane();
        ComboBox combo = new ComboBox();
        List<ComboBoxItem> items = field.getMeta().getDictionary().entrySet().stream().map(entry -> new ComboBoxItem(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        combo.getItems().addAll(items);
        pane.setCenter(combo);
        return pane;
    }

    private Pane createMacAddresField(Field field) {
        BorderPane pane = new BorderPane();
        pane.setCenter(MaskTextField.createMacAddressField());
        return pane;
    }
}
