package com.xored.javafx.packeteditor.view;

import com.xored.javafx.packeteditor.controls.ProtocolField;
import com.xored.javafx.packeteditor.data.Protocol;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class FieldEditorView {
    private Pane parentPane;
    private VBox protocolsPane = new VBox();

    public void setParentPane(Pane parentPane) {
        this.parentPane = parentPane;
    }

    public void addProtocol(Protocol protocol) {
        
        protocolsPane.getChildren().add(buildProtocolRow(protocol));
        protocol.getFields().stream().forEach(field -> protocolsPane.getChildren().add(buildFieldRow(field.getName(), field.getDisplayValue())));
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

    private HBox buildFieldRow(String title, String value) {
        HBox row = new HBox(13);
        row.getStyleClass().addAll("field-row");

        Pane titlePane = new Pane();
        Text titleControl = new Text(title);
        titlePane.getChildren().add(titleControl);
        titlePane.getStyleClass().add("title-pane");

        Pane valuePane = new Pane();

        Hyperlink link = new Hyperlink(value);
        valuePane.getChildren().add(new ProtocolField(value));
        valuePane.getStyleClass().add("value-pane");

        row.getChildren().addAll(titleControl, valuePane);

        return row;
    }
}
