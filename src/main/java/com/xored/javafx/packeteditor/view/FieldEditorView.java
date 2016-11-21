package com.xored.javafx.packeteditor.view;

import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.controls.FEInstructionParameterField;
import com.xored.javafx.packeteditor.controls.FeParameterField;
import com.xored.javafx.packeteditor.controls.ProtocolField;
import com.xored.javafx.packeteditor.data.*;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocol;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocolModel;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import com.xored.javafx.packeteditor.scapy.TCPOptionsData;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.*;

public class FieldEditorView {
    @Inject
    FieldEditorController controller;

    private StackPane fieldEditorPane;
    private StackPane fieldEnginePane;
    
    private Logger logger = LoggerFactory.getLogger(FieldEditorView.class);

    @Inject
    @Named("resources")
    ResourceBundle resourceBundle;

    @Inject
    Injector injector;

    private AutoCompletionBinding<String> protoAutoCompleter;

    public List<TitledPane> getProtocolTitledPanes() {
        return protocolTitledPanes;
    }

    private List<TitledPane> protocolTitledPanes = new ArrayList<>();

    // For even/odd background, this is NOT any real field index
    static private int oddIndex = 0;

    // Current selected field
    static private HBox selected_row = null;

    static void setSelectedRow(HBox row) {
        if (selected_row != row) {
            if (selected_row != null) {
                selected_row.setStyle("-fx-background-color: -trex-field-row-background;");
            }
            selected_row = row;
            selected_row.setStyle("-fx-background-color: -trex-field-row-background-selected;");
        }
    }

    public static void initCss(Scene scene) {
        scene.getStylesheets().add(ClassLoader.getSystemResource("styles/modena-packet-editor.css").toExternalForm());

        Set<String> fontFamilies = javafx.scene.text.Font.getFamilies().stream().collect(Collectors.toSet());

        // Try choose best available fonts with a fallback
        String fontsCssFile;
        if (fontFamilies.contains("Menlo")) {
            fontsCssFile = "main-font-menlo.css";
        } else if (fontFamilies.contains("Lucida Console")) {
            fontsCssFile = "main-font-lucida-console.css";
        } else if (fontFamilies.contains("Consolas")) {
            fontsCssFile = "main-font-consolas.css";
        } else if (fontFamilies.contains("Courier New")) {
            fontsCssFile = "main-font-courier-new.css";
        } else {
            fontsCssFile = "main-font-monospace.css";
        }

        scene.getStylesheets().add(ClassLoader.getSystemResource("styles/" + fontsCssFile).toExternalForm());

        if (System.getenv("DEBUG") == null) {
            scene.getStylesheets().add(ClassLoader.getSystemResource("styles/main-narrow.css").toExternalForm());
        } else {
            // use css from source file to utilize JavaFX css auto-reload
            String cssSource = "file://" + new File("src/main/resources/styles/main-narrow.css").getAbsolutePath();
            scene.getStylesheets().add(cssSource);
        }
    }

    public void setFieldEditorPane(StackPane fieldEditorPane) {
        this.fieldEditorPane = fieldEditorPane;
    }
    public void setFieldEnginePane(StackPane fieldEnginePane) {
        this.fieldEnginePane = fieldEnginePane;
    }

    private MenuItem addMenuItem(ContextMenu ctxMenu, String text, EventHandler<ActionEvent> action) {
        MenuItem menuItem = new MenuItem();
        menuItem.setText(text);
        menuItem.setOnAction(action);
        ctxMenu.getItems().add(menuItem);
        return menuItem;
    }

    public TitledPane buildProtocolPane(CombinedProtocol protocol) {
        TitledPane gridTitlePane = new TitledPane();
        String protocolPathId = protocol.getPath().stream().collect(Collectors.joining("-"));
        gridTitlePane.setId(protocolPathId + "-pane");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("protocolgrid");

        final int[] ij = {0, 0}; // col, row

        oddIndex = 0;
        protocol.getFields().stream().forEach(field -> {
            FieldMetadata meta = field.getMeta();
            FieldMetadata.FieldType type = meta.getType();
            List<Node> list;

            list = buildFieldRow(field);
            
            boolean hasVMInstructions = !field.getFEInstructionParameters().isEmpty();
            for (Node n : list) {
                grid.add(n, ij[0]++, ij[1], 1, 1);
                if (BITMASK.equals(type)
                        || TCP_OPTIONS.equals(type)
                        || BYTES.equals(type)
                        || hasVMInstructions) {
                    ij[0] = 0;
                    ij[1]++;
                }
            }
            ij[0] = 0;
            ij[1]++;
        });
        String title = protocol.getMeta().getName();
        UserProtocol userProtocol = protocol.getUserProtocol();
        ProtocolData protocolData = protocol.getScapyProtocol();
        String subtype = null;
        if (userProtocol != null && protocolData == null) {
            subtype = "as Payload";
            gridTitlePane.getStyleClass().add("invalid-protocol");
        } else if (userProtocol != null && protocolData != null && protocolData.isInvalidStructure()) {
            if (protocolData.getRealId() == null) {
                subtype = "as Payload";
            } else {
                subtype = "as " + protocolData.getRealId();
            }
            gridTitlePane.getStyleClass().add("invalid-protocol");
        } else if (userProtocol != null && protocolData != null && protocolData.protocolRealIdDifferent() ) {
            subtype = "as " + protocolData.getRealId();
        }

        if (subtype != null) {
            title = title + " " + subtype;
        }

        gridTitlePane.setText(title);
        gridTitlePane.setContent(grid);

        if (userProtocol != null) {
            gridTitlePane.setExpanded(!userProtocol.isCollapsed());
            gridTitlePane.expandedProperty().addListener(val->
                    userProtocol.setCollapsed(!gridTitlePane.isExpanded())
            );

            FieldEditorModel model = controller.getModel();
            if (!model.isBinaryMode() && model.getUserModel().getProtocolStack().indexOf(userProtocol) > 0) {
                ContextMenu layerCtxMenu = new ContextMenu();
                addMenuItem(layerCtxMenu, "Move Layer Up", e -> model.moveLayerUp(userProtocol));
                addMenuItem(layerCtxMenu, "Move Layer Down", e -> model.moveLayerDown(userProtocol));
                addMenuItem(layerCtxMenu, "Delete layer", e -> model.removeLayer(userProtocol));
                gridTitlePane.setContextMenu(layerCtxMenu);
            }
        }

        return gridTitlePane;
    }

    public TitledPane buildFETitlePane(String title, Node content) {
        TitledPane pane = new TitledPane();
        pane.setText(title);
        pane.getStyleClass().add("append-protocol");
        pane.setId("fe-parameters-pane");
        pane.setContent(content);
        pane.setCollapsible(false);
        return pane;
    }

    public void buildFieldEnginePane() {

        VBox instructionsPaneVbox = new VBox(20);
        
        //        parametersGrid.add(addInstructionPane, 0, row, 3, 1)
        
        HBox addInstructionPane = new HBox(10);
        ComboBox<InstructionExpressionMeta> instructionSelector = new ComboBox<>();
        instructionSelector.setEditable(true);
        List<InstructionExpressionMeta> items = controller.getMetadataService().getFeInstructions().entrySet().stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        instructionSelector.getItems().addAll(items);
        Button newInstructionBtn = new Button("Add");
        newInstructionBtn.setOnAction(e -> {
            InstructionExpressionMeta selected = instructionSelector.getSelectionModel().getSelectedItem();
            controller.getModel().addInstruction(selected);
        });
        addInstructionPane.getChildren().addAll(new Text("Select: "), instructionSelector, newInstructionBtn);
        
        VBox parametersPane = new VBox();
        GridPane parametersGrid = new GridPane();
        parametersGrid.setVgap(5);
        parametersGrid.getColumnConstraints().addAll(new ColumnConstraints(140),new ColumnConstraints(100),new ColumnConstraints(120));
        int row = 0;
        for(FeParameter feParameter: controller.getModel().getUserModel().getFePrarameters()) {
            int rowId = row++;
            Node label = new Label(feParameter.getName());
            parametersGrid.add(label, 1, rowId, 2, 1);
            GridPane.setHalignment(label, HPos.LEFT);
            FeParameterField parameterField = injector.getInstance(FeParameterField.class);
            parameterField.init(feParameter);
            parametersGrid.add(parameterField, 2, rowId);
//            GridPane.setFillWidth(parameterField, true);
        }
        parametersPane.getChildren().addAll(parametersGrid, addInstructionPane);
        instructionsPaneVbox.getChildren().add(buildFETitlePane("", parametersPane));

        row = 0;
        GridPane instructionsGrid = new GridPane();
        for(InstructionExpression instruction: controller.getModel().getInstructionExpressions()) {
            row = renderInstruction(instructionsGrid, row, instruction);
        }

        instructionsPaneVbox.getChildren().add(buildFETitlePane("Instructions", instructionsGrid));
        
        fieldEnginePane.getChildren().setAll(instructionsPaneVbox);
    }
    
    private int renderInstruction(GridPane grid, int rowIdx, InstructionExpression instruction) {
        // Instruction name
        Text instructionName = new Text(instruction.getId());
        instructionName.setOnMouseReleased(e -> {
            PopOver popOver = new PopOver();
            Text help = new Text(instruction.getHelp());
            popOver.setContentNode(help);
            popOver.setTitle("Help - " + instruction.getId());
            popOver.setAnimated(true);
            popOver.show(instructionName);
        });
        FlowPane instructionNamePane = new FlowPane();
        instructionNamePane.getChildren().addAll(instructionName, new Text("("));
        grid.add(instructionNamePane, 0, rowIdx++);
         
        // Add parameters
        for(FEInstructionParameter2 parameter : instruction.getParameters()) {
            HBox parameterPane = getParameterPane();
            
            FEInstructionParameterField instructionField = injector.getInstance(FEInstructionParameterField.class);
            instructionField.init(parameter);
            instructionField.setInstruction(instruction);

            parameterPane.getChildren().addAll(new Text(parameter.getId()), new Text("="), instructionField);
            grid.add(parameterPane, 0, rowIdx++);
        }
        
        grid.add(new Text(")"), 0, rowIdx++);
        grid.add(new Text(""), 0, rowIdx++);
        return rowIdx;
    }

    private HBox getParameterPane() {
        HBox parameterPane = new HBox(5);
        parameterPane.setPrefHeight(20);
        parameterPane.setPadding(new Insets(0, 0, 0, 30));
        return parameterPane;
    }

    private FEInstructionParameter2 createAppendParameter(List<FEInstructionParameter2> parameters) {
        Map<String, String> dict = parameters.stream()
                .collect(Collectors.toMap(FEInstructionParameter2::getId, FEInstructionParameter2::getId));
        FEInstructionParameterMeta meta = new FEInstructionParameterMeta("ENUM", "appendParam", "appendParam", "more", dict, false);
        
        return new FEInstructionParameter2(meta, new JsonPrimitive(meta.getDefaultValue()));
    }
    
    public void rebuild(CombinedProtocolModel model) {
        try {
            protocolTitledPanes = new ArrayList<>();

            model.getProtocolStack().stream().forEach(proto -> {
                protocolTitledPanes.add(buildProtocolPane(proto));
            });

            VBox protocolsPaneVbox = new VBox();
            protocolsPaneVbox.getChildren().setAll(protocolTitledPanes);
            fieldEditorPane.getChildren().setAll(protocolsPaneVbox);

            buildFieldEnginePane();
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
            int end = begin + Math.max(len - 1, 0);

            if (len > 0) {
                lblInfo.setText(String.format("%04x-%04x [%04d]", begin, end, len));
            } else {
                lblInfo.setText(String.format("%04x-%04x [bits]", begin, end));
            }
        } else {
            lblInfo.setText("meta-field");
        }

        lblInfo.setOnMouseClicked(e -> controller.selectField(field));
        lblName.setOnMouseClicked(e -> controller.selectField(field));
        lblName.setTooltip(new Tooltip(field.getMeta().getId()));
        lblName.setId(getUniqueIdFor(field) + "-label");

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
        row.setOnMouseClicked(e -> setSelectedRow(row));

        String even = "-even";
        if (!BYTES.equals(type) && oddIndex % 2 != 0) {
            even = "-odd";
        }
        row.getStyleClass().addAll("field-row" + even);
        oddIndex++;

        BorderPane titlePane = new BorderPane();
        titlePane.setLeft(getFieldLabel(field));
        titlePane.getStyleClass().add("title-pane");

        ProtocolField fieldControl = injector.getInstance(ProtocolField.class);
        fieldControl.init(field);

        BorderPane valuePane = new BorderPane();
        valuePane.setCenter(fieldControl);
        row.getChildren().addAll(titlePane, valuePane);
        rows.add(row);
        row.setOnContextMenuRequested(e->{
            ContextMenu contextMenu = fieldControl.getContextMenu();
            if (contextMenu != null) {
                e.consume();
                contextMenu.show(row, e.getScreenX(), e.getScreenY());
            }
        });

        field.getFEInstructionParameters().stream().forEach(feInstructionParameter -> rows.add(createFEInstructionFieldRow(feInstructionParameter)));
        
        if(BITMASK.equals(type)) {
            field.getMeta().getBits().stream().forEach(bitFlagMetadata -> {
                rows.add(this.createBitFlagRow(field, bitFlagMetadata));
            });
        }
        if(TCP_OPTIONS.equals(type) && field.getScapyFieldData() != null) {
            TCPOptionsData.fromFieldData(field.getScapyFieldData()).stream().forEach(fd -> {
                rows.add(createTCPOptionRow(field, fd));
            });
        }

        return rows;
    }

    private Node createFEInstructionFieldRow(FEInstructionParameter instructionParameter) {
        FEInstructionParameterMeta instructionParameterMeta = instructionParameter.getMeta();
        Node label = buildIndentedFieldLabel("field engine", instructionParameterMeta.getName(), false);
        FEInstructionParameterField editableField = injector.getInstance(FEInstructionParameterField.class);
//        editableField.init(instructionParameter);
        return createRow(label, editableField, null);
    }

    private Node createTCPOptionRow(CombinedField field, TCPOptionsData tcpOption) {
        // TODO: reuse code
        BorderPane titlePane = new BorderPane();
        titlePane.setLeft(buildIndentedFieldLabel("", tcpOption.getName()));
        titlePane.getStyleClass().add("title-pane");
        titlePane.setOnMouseClicked(e -> controller.selectField(field));

        HBox row = new HBox();
        row.setOnMouseClicked(e -> setSelectedRow(row));
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
        String flagName = bitFlagMetadata.getName();
        int flagMask = bitFlagMetadata.getMask();
        Node label = buildIndentedFieldLabel(maskToString(flagMask), flagName, true);
        ComboBox<ComboBoxItem> combo = createBitFlagComboBox(field, bitFlagMetadata, flagName, flagMask);

        return createRow(label, combo, field);
    }

    private Node createRow(Node label, Node control, CombinedField field) {
        BorderPane titlePane = new BorderPane();
        
        titlePane.setLeft(label);
        titlePane.getStyleClass().add("title-pane");
        if (field != null) {
            titlePane.setOnMouseClicked(e -> controller.selectField(field));
        }

        HBox row = new HBox();
        row.setOnMouseClicked(e -> setSelectedRow(row));
        row.getStyleClass().addAll("field-row-flags");
        if (oddIndex %2 == 0) oddIndex++;

        BorderPane valuePane = new BorderPane();
        valuePane.setLeft(control);
        row.getChildren().addAll(titlePane, valuePane);
        return row;
    }
    
    private ComboBox<ComboBoxItem> createBitFlagComboBox(CombinedField field, BitFlagMetadata bitFlagMetadata, String flagName, int flagMask) {
        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.setId(getUniqueIdFor(field));
        combo.getStyleClass().setAll("control", "bitflag");

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
        return combo;
    }

    public void displayConnectionError() {
        ConnectionErrorDialog dialog = new ConnectionErrorDialog();
        dialog.showAndWait();
    }

    public void showEmptyPacketContent() {
        BorderPane emptyPacketPane = new BorderPane();
        emptyPacketPane.setCenter(new Label("Empty packet. Please click to add default protocol."));
        
        emptyPacketPane.setOnMouseClicked((event) -> controller.newPacket());
        
        fieldEditorPane.getChildren().add(emptyPacketPane);
    }

    public void reset() {
        fieldEditorPane.getChildren().clear();
        showEmptyPacketContent();
    }
}
