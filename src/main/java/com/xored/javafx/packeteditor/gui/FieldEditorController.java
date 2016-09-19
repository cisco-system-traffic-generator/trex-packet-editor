package com.xored.javafx.packeteditor.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xored.javafx.packeteditor.data.BinaryData;
import com.xored.javafx.packeteditor.data.Field;
import com.xored.javafx.packeteditor.data.IBinaryData;
import com.xored.javafx.packeteditor.data.PacketDataController;
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

    @Inject
    private PacketDataController packetController;

    TreeTableView<Field> treeTableView;

    public TreeItem<Field> buildTree() {
        List<TreeItem<Field>> treeItems = new ArrayList<>();
        Iterator it = packetController.getProtocols().iterator();
        while(it.hasNext()) {
            int protocolLength = 0;
            JsonObject protocol = (JsonObject) it.next();
            String protocolId = protocol.get("id").getAsString();
            JsonArray fields = protocol.getAsJsonArray("fields");
            Iterator fieldsIt = fields.iterator();
            List<TreeItem<Field>> fieldItems = new ArrayList<>();
            Integer protocolOffset = protocol.get("offset").getAsInt();
            while (fieldsIt.hasNext()) {
                JsonObject field =(JsonObject) fieldsIt.next();
                String fieldName = field.get("name").getAsString();
                Integer offset = field.get("offset").getAsInt();
                Integer length = field.get("length").getAsInt();
                String value = field.get("value").getAsString();
                protocolLength += length;
                fieldItems.add(new TreeItem<>(new Field(fieldName, offset, length, protocolOffset, value, Field.Type.STRING)));
            }
            
            Field protocolField = new Field(protocolId, protocolOffset, protocolLength, 0, null, Field.Type.PROTOCOL);
            TreeItem<Field> protocolTreeItem = new TreeItem<>(protocolField);
            protocolTreeItem.setExpanded(true);
            protocolTreeItem.getChildren().addAll(fieldItems);
            treeItems.add(protocolTreeItem);
        }

        final TreeItem<Field> root = new TreeItem<>(new Field("Root", 0, 0, 0, null, Field.Type.NONE));
        root.setExpanded(true);
        root.getChildren().addAll(treeItems);

        return root;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        packetController.addObserver(this);
        packetController.init();

        binaryData.getObservable().addObserver(this);
    }

    private void rebuildTreeView() {
        final TreeItem<Field> root = buildTree();

        TreeTableColumn<Field, String> dataColumn = new TreeTableColumn<>("Field");
        dataColumn.setPrefWidth(150);
        dataColumn
                .setCellValueFactory((
                        TreeTableColumn.CellDataFeatures<Field, String> param) -> new ReadOnlyStringWrapper(
                            param.getValue().getValue().getName()));

        TreeTableColumn<Field, String> valueColumn = new TreeTableColumn<>(
                "Value");
        valueColumn.setPrefWidth(190);

        Callback<TreeTableColumn<Field, String>, TreeTableCell<Field, String>> cellFactory
                = (TreeTableColumn<Field, String> param) -> new EditingCell();

        valueColumn.setCellFactory(cellFactory);
        valueColumn
                .setCellValueFactory(
                        (TreeTableColumn.CellDataFeatures<Field, String> param) -> {
                            Field field = param.getValue().getValue();
                            if (field.getName().startsWith("Ethernet") || field.getName().startsWith("IPv4")) {
                                return new ReadOnlyStringWrapper("");
                            }
                            return new SimpleStringProperty(getFieldStringValue(field));
                        });

        valueColumn.setOnEditCommit(
                (TreeTableColumn.CellEditEvent<Field, String> t) -> {
                    Field f = (Field) t.getTreeTableView().getTreeItem(t.getTreeTablePosition().getRow()).getValue();
                    String newValue = t.getNewValue();
                    byte[] bytes = parseFieldBytes(f, newValue);
                    if (bytes.length > 0) {
                        binaryData.setBytes(f.getOffset(), f.getLength(), bytes);
                    }
                });

        treeTableView = new TreeTableView<>(root);
        treeTableView.getColumns().setAll(dataColumn, valueColumn);

        treeTableView.getSelectionModel().selectedItemProperty().addListener( new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue,
                                Object newValue) {

                TreeItem<Field> selectedItem = (TreeItem<Field>) newValue;
                binaryData.setSelected(selectedItem.getValue().getAbsOffset(), selectedItem.getValue().getLength());
            }

        });

        treeTableView.setEditable(true);
        treeTableView.setShowRoot(false);

        fieldEditorPane.getChildren().clear();
        fieldEditorPane.getChildren().add(treeTableView);
    }

    private String getFieldStringValue(Field field) {
        byte[] bytes = binaryData.getBytes(field.getAbsOffset(), field.getLength());
        /*
        String result = "";
        if (field.getType() == Field.Type.BINARY) {
            int value = 0;
            for (int i = 0; i < bytes.length; i++) {
                value |= (int) (bytes[bytes.length - 1 - i] & 0xFF) << i * 8;
            }

            return String.format("%d", value);
        } else if (field.getType() == Field.Type.MAC_ADDRESS) {
            return String.format("%02X:%02X:%02X:%02X:%02X:%02X", bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5]);
        } else if (field.getType() == Field.Type.IP_ADDRESS) {
            return String.format("%d.%d.%d.%d", (0x000000FF & (int)bytes[0]), (0x000000FF & (int)bytes[1]), (0x000000FF & (int)bytes[2]), (0x000000FF & (int)bytes[3]));
        }
        return result;
        */
        return field.getDisplayValue();
    }

    private byte[] parseFieldBytes(Field field, String value) {
        byte[] bytes = new byte[0];
        if (field.getType() == Field.Type.BINARY) {
            if (field.getLength() == 4) {
                bytes = ByteBuffer.allocate(4).putInt(Integer.parseInt(value)).array();
            } else {
                bytes = ByteBuffer.allocate(2).putShort(Short.parseShort(value)).array();
            }
        } else if (field.getType() == Field.Type.MAC_ADDRESS) {
            try {
                ByteBuffer bf = ByteBuffer.allocate(6);
                String[] parts = value.split(":");

                bf.put((byte) (Integer.parseInt(parts[0], 16)));
                bf.put((byte) (Integer.parseInt(parts[1], 16)));
                bf.put((byte) (Integer.parseInt(parts[2], 16)));
                bf.put((byte) (Integer.parseInt(parts[3], 16)));
                bf.put((byte) (Integer.parseInt(parts[4], 16)));
                bf.put((byte) (Integer.parseInt(parts[5], 16)));

                bytes = bf.array();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (field.getType() == Field.Type.IP_ADDRESS) {
            try {
                ByteBuffer bf = ByteBuffer.allocate(4);
                String[] parts = value.split("\\.");

                bf.put((byte) (Integer.parseInt(parts[0])));
                bf.put((byte) (Integer.parseInt(parts[1])));
                bf.put((byte) (Integer.parseInt(parts[2])));
                bf.put((byte) (Integer.parseInt(parts[3])));

                bytes = bf.array();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return bytes;
    }

    public void setFieldEditorPane(StackPane fieldEditorPane) {
        this.fieldEditorPane = fieldEditorPane;
    }

    @Override
    public void update(Observable o, Object arg) {
        if ((o == binaryData) && (BinaryData.OP.SET_BYTE.equals(arg))) {
            treeTableView.refresh();
        } else if (o == packetController) {
            rebuildTreeView();
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
