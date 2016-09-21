package com.xored.javafx.packeteditor.controllers;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.xored.javafx.packeteditor.data.OldField;
import com.xored.javafx.packeteditor.data.FieldEditorModel;
import com.xored.javafx.packeteditor.data.Protocol;
import com.xored.javafx.packeteditor.events.FieldEvent;
import com.xored.javafx.packeteditor.events.ProtocolEvent;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.StackPane;

import javax.inject.Inject;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

public class FieldEditorController2 implements Initializable, Observer {
    @FXML private StackPane fieldEditorPane;
    
    @Inject
    FieldEditorModel model;
    
    @Inject
    EventBus eventBus;
    
    @Inject
    FieldEditorView view;
    
//    @Inject
//    private IBinaryData binaryData;

//    @Inject
//    private PacketDataController packetController;
//
//    TreeTableView<OldField> treeTableView;
//
//    public TreeItem<OldField> buildTree() {
//        
//        final TreeItem<OldField> root = new TreeItem<>(new OldField("Root", 0, 0, 0, null, OldField.Type.NONE));
//        
//        return root;
//    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        view.setParentPane(fieldEditorPane);
    }

    @Subscribe
    public void handleFiledEvent(FieldEvent event) {
        FieldEvent.Action action = event.getAction();
        FieldMetadata meta = event.getFieldMetadata();
        OldField oldField = (OldField) event.getValue();
    }

    @Subscribe
    public void handleProtocolEvent(ProtocolEvent event) {
        ProtocolEvent.Action action = event.getAction();
        ProtocolMetadata meta = event.getProtocolMetadata();
        Protocol protocol = (Protocol) event.getValue();
        view.addProtocol(protocol);
    }
    
    private void rebuildTreeView() {
//        final TreeItem<OldField> root = buildTree();
//
//        TreeTableColumn<OldField, String> dataColumn = new TreeTableColumn<>("OldField");
//        dataColumn.setPrefWidth(150);
//        dataColumn
//                .setCellValueFactory((
//                        TreeTableColumn.CellDataFeatures<OldField, String> param) -> new ReadOnlyStringWrapper(
//                            param.getValue().getValue().getName()));
//
//        TreeTableColumn<OldField, String> valueColumn = new TreeTableColumn<>(
//                "Value");
//        valueColumn.setPrefWidth(190);
//
//        Callback<TreeTableColumn<OldField, String>, TreeTableCell<OldField, String>> cellFactory
//                = (TreeTableColumn<OldField, String> param) -> new EditingCell();
//
//        valueColumn.setCellFactory(cellFactory);
//        valueColumn
//                .setCellValueFactory(
//                        (TreeTableColumn.CellDataFeatures<OldField, String> param) -> {
//                            OldField field = param.getValue().getValue();
//                            if (field.getName().startsWith("Ethernet") || field.getName().startsWith("IPv4")) {
//                                return new ReadOnlyStringWrapper("");
//                            }
//                            return new SimpleStringProperty(getFieldStringValue(field));
//                        });
//
//        valueColumn.setOnEditCommit(
//                (TreeTableColumn.CellEditEvent<OldField, String> t) -> {
//                    OldField f = (OldField) t.getTreeTableView().getTreeItem(t.getTreeTablePosition().getRow()).getValue();
//                    String newValue = t.getNewValue();
//                    byte[] bytes = parseFieldBytes(f, newValue);
//                    if (bytes.length > 0) {
//                        binaryData.setBytes(f.getOffset(), f.getLength(), bytes);
//                    }
//                });
//
//        treeTableView = new TreeTableView<>(root);
//        treeTableView.getColumns().setAll(dataColumn, valueColumn);
//
//        treeTableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
//
//            @Override
//            public void changed(ObservableValue observable, Object oldValue,
//                                Object newValue) {
//
//                TreeItem<OldField> selectedItem = (TreeItem<OldField>) newValue;
//                binaryData.setSelected(selectedItem.getValue().getAbsOffset(), selectedItem.getValue().getLength());
//            }
//
//        });
//
//        treeTableView.setEditable(true);
//        treeTableView.setShowRoot(false);
//
//        fieldEditorPane.getChildren().clear();
//        fieldEditorPane.getChildren().add(treeTableView);
    }

    private String getFieldStringValue(OldField oldField) {
        /* byte[] bytes = binaryData.getBytes(oldField.getAbsOffset(), oldField.getLength());
        
        String result = "";
        if (oldField.getType() == OldField.Type.BINARY) {
            int value = 0;
            for (int i = 0; i < bytes.length; i++) {
                value |= (int) (bytes[bytes.length - 1 - i] & 0xFF) << i * 8;
            }

            return String.format("%d", value);
        } else if (oldField.getType() == OldField.Type.MAC_ADDRESS) {
            return String.format("%02X:%02X:%02X:%02X:%02X:%02X", bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5]);
        } else if (oldField.getType() == OldField.Type.IP_ADDRESS) {
            return String.format("%d.%d.%d.%d", (0x000000FF & (int)bytes[0]), (0x000000FF & (int)bytes[1]), (0x000000FF & (int)bytes[2]), (0x000000FF & (int)bytes[3]));
        }
        return result;
        */
        return oldField.getDisplayValue();
    }

    private byte[] parseFieldBytes(OldField oldField, String value) {
        byte[] bytes = new byte[0];
        if (oldField.getType() == OldField.Type.BINARY) {
            if (oldField.getLength() == 4) {
                bytes = ByteBuffer.allocate(4).putInt(Integer.parseInt(value)).array();
            } else {
                bytes = ByteBuffer.allocate(2).putShort(Short.parseShort(value)).array();
            }
        } else if (oldField.getType() == OldField.Type.MAC_ADDRESS) {
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
        } else if (oldField.getType() == OldField.Type.IP_ADDRESS) {
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
//        if ((o == binaryData) && (BinaryData.OP.SET_BYTE.equals(arg))) {
//            treeTableView.refresh();
//        } else if (o == packetController) {
//            rebuildTreeView();
//        }
    }


    class EditingCell extends TreeTableCell<OldField, String> {

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
