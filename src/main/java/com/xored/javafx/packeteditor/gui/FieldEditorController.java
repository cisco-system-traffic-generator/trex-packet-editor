package com.xored.javafx.packeteditor.gui;

import com.xored.javafx.packeteditor.data.BinaryData;
import com.xored.javafx.packeteditor.data.Field;
import com.xored.javafx.packeteditor.data.IBinaryData;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import javax.inject.Inject;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

public class FieldEditorController implements Initializable, Observer {
    @FXML private StackPane fieldEditorPane;
    @Inject
    private IBinaryData binaryData;

    List<Field> fields = Arrays.<Field> asList(
        new Field(0, 2), new Field(2, 2), new Field(4, 4),
        new Field(8, 2), new Field(10, 4), new Field(14, 2)
    );

    final TreeItem<Field> root = new TreeItem<>(new Field(0, 16));
    TreeTableView<Field> treeTableView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.setExpanded(true);
        fields.stream().forEach((field) -> {
            root.getChildren().add(new TreeItem<>(field));
        });

        TreeTableColumn<Field, String> dataColumn = new TreeTableColumn<>(
                "Field");
        dataColumn.setPrefWidth(150);
        dataColumn
                .setCellValueFactory((
                        TreeTableColumn.CellDataFeatures<Field, String> param) -> new ReadOnlyStringWrapper(
                        "[" + param.getValue().getValue().getOffset() + ", "
                                + (param.getValue().getValue().getOffset() + param.getValue().getValue().getLength())+ "]"));

        TreeTableColumn<Field, String> valueColumn = new TreeTableColumn<>(
                "value");
        valueColumn.setPrefWidth(190);

        Callback<TreeTableColumn<Field, String>, TreeTableCell<Field, String>> cellFactory
                = (TreeTableColumn<Field, String> param) -> new EditingCell();

        valueColumn.setCellFactory(cellFactory);
        valueColumn
                .setCellValueFactory(
                    (TreeTableColumn.CellDataFeatures<Field, String> param) -> {
                    Field field = param.getValue().getValue();
                    byte[] bytes = binaryData.getBytes(field.getOffset(), field.getLength());
                    return new SimpleStringProperty(bytesToString(bytes));
                });

        valueColumn.setOnEditCommit(
                (TreeTableColumn.CellEditEvent<Field, String> t) -> {
                    Field f = (Field) t.getTreeTableView().getTreeItem(t.getTreeTablePosition().getRow()).getValue();
                    String newValue = t.getNewValue();
                    byte[] bytes;
                    if (f.getLength() == 4) {
                        bytes = ByteBuffer.allocate(4).putInt(Integer.parseInt(newValue)).array();
                    } else {
                        bytes = ByteBuffer.allocate(2).putShort(Short.parseShort(newValue)).array();
                    }
                    binaryData.setBytes(f.getOffset(), f.getLength(), bytes);
                });

        treeTableView = new TreeTableView<>(root);
        treeTableView.getColumns().setAll(dataColumn, valueColumn);

        treeTableView.getSelectionModel().selectedItemProperty().addListener( new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue,
                                Object newValue) {

                TreeItem<Field> selectedItem = (TreeItem<Field>) newValue;
                binaryData.setSelected(selectedItem.getValue().getOffset(), selectedItem.getValue().getLength());
            }

        });

        treeTableView.setEditable(true);

        fieldEditorPane.getChildren().add(treeTableView);

        binaryData.getObservable().addObserver(this);
    }

    private String bytesToString(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value |= (int)(bytes[bytes.length - 1 - i ] & 0xFF) << i*8;
        }

        return String.format("%d", value);
    }

    public void setFieldEditorPane(StackPane fieldEditorPane) {
        this.fieldEditorPane = fieldEditorPane;
    }

    @Override
    public void update(Observable o, Object arg) {
        if ((o == binaryData) && (BinaryData.OP.SET_BYTE.equals(arg))) {
            treeTableView.refresh();
        }
    }


    class EditingCell extends TreeTableCell<Field, String> {

        private TextField textField;

        private EditingCell() {
        }

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();

            setText((String) getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(item);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
//                        setGraphic(null);
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            textField.setOnAction((e) -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if (!newValue) {
                    System.out.println("Commiting " + textField.getText());
                    commitEdit(textField.getText());
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }

}
