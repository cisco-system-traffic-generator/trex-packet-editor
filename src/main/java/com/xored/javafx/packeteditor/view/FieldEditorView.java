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
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.TCPOptionsData;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.*;

public class FieldEditorView {
    @Inject
    FieldEditorController controller;

    private StackPane fieldEditorPane;
    
    private VBox protocolsPane = new VBox();
    
    private Logger logger = LoggerFactory.getLogger(FieldEditorView.class);

    @Inject
    @Named("resources")
    ResourceBundle resourceBundle;

    @Inject
    Injector injector;

    public void setParentPane(StackPane parentPane) {
        this.fieldEditorPane = parentPane;
    }


    public TitledPane buildProtocolPane(CombinedProtocol protocol) {

        TitledPane gridTitlePane = new TitledPane();
        
        GridPane grid = new GridPane();
        grid.getStyleClass().add("protocolgrid");

        final int[] ij = {0, 0}; // col, row

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
        if (protocol.getUserProtocol() != null && protocol.getScapyProtocol() == null) {
            title = title + "(as Raw payload)";
            gridTitlePane.getStyleClass().add("invalid-protocol");
        }
        gridTitlePane.setText(title);
        gridTitlePane.setContent(grid);

        return gridTitlePane;
    }

    public TitledPane buildAppendProtocolPane() {
        TitledPane pane = new TitledPane();
        pane.setText("Append layer");
        pane.getStyleClass().add("append-protocol");
        HBox controls = new HBox(10);
        pane.setContent(controls);

        List<ProtocolMetadata> protocols = controller.getModel().getAvailableProtocolsToAdd(false);
        if (protocols.isEmpty()) {
            pane.setExpanded(false);
        }
        ComboBox<ProtocolMetadata> cb = new ComboBox<>();
        cb.getStyleClass().add("layer-type-selector");
        cb.setEditable(true);
        cb.getItems().addAll(protocols);

        // Display only available protocols, but let user choose any
        List<String> protoIds = controller.getMetadataService().getProtocols().values().stream()
                .map(ProtocolMetadata::getId)
                .sorted()
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
        HBox.setHgrow(cb, Priority.ALWAYS);
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

        lblInfo.setOnMouseClicked(e -> controller.selectField(field));
        lblName.setOnMouseClicked(e -> controller.selectField(field));
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
        row.getStyleClass().addAll("field-row");

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
        row.getStyleClass().addAll("field-row");

        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.setId(getUniqueIdFor(field));
        combo.getStyleClass().addAll("control", "bitflag");
        
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
}
