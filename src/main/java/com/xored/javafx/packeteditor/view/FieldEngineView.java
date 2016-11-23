package com.xored.javafx.packeteditor.view;

import com.xored.javafx.packeteditor.controls.FEInstructionParameterField;
import com.xored.javafx.packeteditor.controls.FeParameterField;
import com.xored.javafx.packeteditor.data.FEInstructionParameter2;
import com.xored.javafx.packeteditor.data.FeParameter;
import com.xored.javafx.packeteditor.data.InstructionExpression;
import com.xored.javafx.packeteditor.data.PacketEditorModel;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.combined.CombinedProtocol;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.controlsfx.control.PopOver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldEngineView extends FieldEditorView {

    public void rebuild() {
        try {
            List<Node> layers = new ArrayList<>();

            layers.add(buildAddInstructionLayer());

            getModel().getInstructionExpressions().stream().forEach(instruction -> {
//                layers.add(buildLayer(proto));
            });

            VBox protocolsPaneVbox = new VBox(20);
            protocolsPaneVbox.getChildren().setAll(layers);
            rootPane.getChildren().setAll(protocolsPaneVbox);
        } catch(Exception e) {
            logger.error("Error occurred during rebuilding view. Error {}", e);
        }
    }

    private Node buildAddInstructionLayer() {
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

        TitledPane layer = new TitledPane();
        layer.setContent(addInstructionPane);
        layer.setExpanded(true);
        layer.setCollapsible(false);

        return layer;
    }
    
    public TitledPane buildLayer(CombinedProtocol protocol) {
        TitledPane layerPane = new TitledPane();
        layerPane.setId(getLayerId(protocol) + "-pane");
        
        layerPane.getStyleClass().add(getLayerStyleClass(protocol));
        
        GridPane layerContent = new GridPane();
        layerContent.getStyleClass().add("protocolgrid");

        int layerRowIdx = 0;

        oddIndex = 0;
        for(Node row : buildLayerRows(protocol)) {
            layerContent.add(row, 0, layerRowIdx++);
        }

        String layerTitle = getLayerTitle(protocol);
        layerPane.setText(layerTitle);
        layerPane.setContent(layerContent);
        configureLayerExpandCollapse(layerPane, protocol);
        layerPane.setContextMenu(getLayerContextMenu(protocol));
        return layerPane;
    }
    
    protected List<Node> buildLayerRows(CombinedProtocol protocol) {
        List<Node> rows = new ArrayList<>();
        return rows;
    }
    
    protected void configureLayerExpandCollapse(TitledPane layerPane, CombinedProtocol protocol) {
        UserProtocol userProtocol = protocol.getUserProtocol();
        if(userProtocol != null) {
            layerPane.setExpanded(!userProtocol.isCollapsed());
            layerPane.expandedProperty().addListener(val->
                    userProtocol.setCollapsed(!layerPane.isExpanded())
            );
        }
    }
    
    protected ContextMenu getLayerContextMenu(CombinedProtocol protocol) {
        UserProtocol userProtocol = protocol.getUserProtocol();    
        PacketEditorModel model = controller.getModel();
        ContextMenu layerCtxMenu = null;
        if (!model.isBinaryMode() && model.getUserModel().getProtocolStack().indexOf(userProtocol) > 0) {
            layerCtxMenu = new ContextMenu();
            addMenuItem(layerCtxMenu, "Move Layer Up", e -> model.moveLayerUp(userProtocol));
            addMenuItem(layerCtxMenu, "Move Layer Down", e -> model.moveLayerDown(userProtocol));
            addMenuItem(layerCtxMenu, "Delete", e -> model.removeLayer(userProtocol));
        }
        return layerCtxMenu;
    }

    private MenuItem addMenuItem(ContextMenu ctxMenu, String text, EventHandler<ActionEvent> action) {
        MenuItem menuItem = new MenuItem();
        menuItem.setText(text);
        menuItem.setOnAction(action);
        ctxMenu.getItems().add(menuItem);
        return menuItem;
    }
    
    private String getLayerId(CombinedProtocol protocol) {
        return protocol.getPath().stream().collect(Collectors.joining("-"));
    }
    
    protected String getLayerStyleClass(CombinedProtocol protocol) {
        UserProtocol userProtocol = protocol.getUserProtocol();
        ProtocolData protocolData = protocol.getScapyProtocol();
        if (userProtocol != null && protocolData != null
            && ( protocolData.isInvalidStructure() || protocolData.protocolRealIdDifferent())) {
            return "invalid-protocol";
        }
        return "";
    }
    
    protected String getLayerTitle (CombinedProtocol protocol) {
        String title = protocol.getMeta().getName();
        UserProtocol userProtocol = protocol.getUserProtocol();
        ProtocolData protocolData = protocol.getScapyProtocol();
        String subtype = null;
        if (userProtocol != null && protocolData == null) {
            subtype = "as Payload";
        } else if (userProtocol != null && protocolData != null && protocolData.isInvalidStructure()) {
            if (protocolData.getRealId() == null) {
                subtype = "as Payload";
            } else {
                subtype = "as " + protocolData.getRealId();
            }
        } else if (userProtocol != null && protocolData != null && protocolData.protocolRealIdDifferent() ) {
            subtype = "as " + protocolData.getRealId();
        }

        if (subtype != null) {
            title = title + " " + subtype;
        }
        return title;
    }
    
    public void buildFieldEnginePane() {

        VBox instructionsPaneVbox = new VBox(20);
        
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
        instructionsPaneVbox.getChildren().add(buildFETitlePane("", parametersPane));

        row = 0;
        GridPane instructionsGrid = new GridPane();
        for(InstructionExpression instruction: controller.getModel().getInstructionExpressions()) {
            row = renderInstruction(instructionsGrid, row, instruction);
        }

        instructionsPaneVbox.getChildren().add(buildFETitlePane("Instructions", instructionsGrid));
        
        rootPane.getChildren().setAll(instructionsPaneVbox);
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
    
    public void showEmptyPacketContent() {
    }

    public void reset() {
        rootPane.getChildren().clear();
        showEmptyPacketContent();
    }
}