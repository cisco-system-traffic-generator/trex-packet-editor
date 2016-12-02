package com.xored.javafx.packeteditor.view;

import com.xored.javafx.packeteditor.controls.FEInstructionParameterField;
import com.xored.javafx.packeteditor.controls.FeParameterField;
import com.xored.javafx.packeteditor.data.FeParameter;
import com.xored.javafx.packeteditor.data.InstructionExpression;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.controlsfx.control.BreadCrumbBar;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static javafx.scene.input.KeyCode.ENTER;

public class FieldEngineView extends FieldEditorView {

    private AutoCompletionBinding<String> instructionAutoCompleter;
    private VBox topPane;
    private VBox bottomPane;
    private ScrollPane scrollPane;

    public void rebuild() {
        try {
            List<Node> layers = new ArrayList<>();

            List<Node> instructionLayers = getModel().getInstructionExpressions().stream().map(this::buildLayerData).collect(Collectors.toList());

            updateTopLayer();
            updateBottomLayer();
            
            layers.addAll(instructionLayers);
            
            VBox protocolsPaneVbox = new VBox();
            protocolsPaneVbox.getChildren().setAll(layers);
            rootPane.getChildren().setAll(protocolsPaneVbox);
        } catch(Exception e) {
            logger.error("Error occurred during rebuilding view. Error {}", e);
        }
    }

    private void updateTopLayer() {
        topPane.getChildren().clear();
        BreadCrumbBar<String> pktStructure = new BreadCrumbBar<>();
        pktStructure.setAutoNavigationEnabled(false);
        pktStructure.setSelectedCrumb(buildPktStructure());
        pktStructure.setPadding(new Insets(10, 10, 10, 10));
        
        TitledPane pktStructureLayer = new TitledPane();
        pktStructureLayer.setCollapsible(false);
        pktStructureLayer.setText("Packet structure");
        pktStructureLayer.setContent(pktStructure);

        topPane.getChildren().addAll(pktStructureLayer);
        
        String error = controller.getModel().getFieldEngineError();
        if(error != null) {

            BorderPane errorPane = new BorderPane();
            errorPane.setLeft(new Text(error));
            TitledPane errorLayer = new TitledPane("Error", errorPane);
            
            errorLayer.getStyleClass().add("invalid-layer");
            errorLayer.setCollapsible(false);
            topPane.getChildren().add(errorLayer);
        }
    }
    
    private TreeItem<String> buildPktStructure() {
        Stack<UserProtocol> protocols = getModel().getUserModel().getProtocolStack();
        TreeItem<String> currentProto = null;
        for(UserProtocol protocol : protocols) {
            if(currentProto == null) {
                currentProto = new TreeItem<>(protocol.getPaddedId());
            } else {
                TreeItem<String> child = new TreeItem<>(protocol.getPaddedId());
                currentProto.getChildren().add(child);
                currentProto = child;
            }
        }
        return currentProto;
    }
    
    protected Node buildLayerData(InstructionExpression instruction) {
        LayerContext layerContext = new LayerContext() {
            @Override
            public String getLayerId() {
                return "field-engine-instruction-" +instruction.getId();
            }

            @Override
            public String getTitle() {
                return instruction.getId();
            }

            @Override
            public String getStyleClass() {
                return "field-engine-instruction";
            }

            @Override
            public List<Node> getRows() {
                return buildLayerRows(instruction);
            }

            @Override
            public ContextMenu getContextMenu() {
                ContextMenu layerCtxMenu = new ContextMenu();
                addMenuItem(layerCtxMenu, "Show help", e -> {
                    StackPane helpContentPane = new StackPane();
                    helpContentPane.setId("instructions-help-pane");
                    helpContentPane.getChildren().add(new Text(instruction.getHelp()));

                    PopOver popOver = new PopOver();
                    popOver.setAutoHide(true);
                    popOver.setContentNode(helpContentPane);

                    popOver.setTitle("Help - " + instruction.getId());
                    popOver.setAnimated(true);
                    
                    Double x = layerCtxMenu.getX();
                    Double y = layerCtxMenu.getY();
                    popOver.show(rootPane, x, y);
                });
                addMenuItem(layerCtxMenu, "Delete", e -> controller.getModel().removeInstructionLayer(instruction));
                return layerCtxMenu;
            }

            @Override
            public boolean isCollapsed() {
                return false;
            }

            @Override
            public void setCollapsed(boolean collapsed) {
            }

            @Override
            public void configureLayerExpandCollapse(TitledPane layerPane) {
            }
        };
        return buildLayer(layerContext);
    }

    private void updateBottomLayer() {
        bottomPane.getChildren().clear();
        HBox addInstructionPane = new HBox(10);
        ComboBox<InstructionExpressionMeta> instructionSelector = new ComboBox<>();
        instructionSelector.setEditable(true);
        
        Map<String, InstructionExpressionMeta> instructionExpressionMetas = controller.getMetadataService().getFeInstructions();
        instructionSelector.getItems().addAll(instructionExpressionMetas.values());
        
        List<String> instructionIds = instructionExpressionMetas.values().stream()
                .map(InstructionExpressionMeta::getId)
                .collect(Collectors.toList());
        
        instructionAutoCompleter = TextFields.bindAutoCompletion(instructionSelector.getEditor(), instructionIds);
        instructionSelector.setOnHidden((e) -> {
            if (instructionAutoCompleter == null) {
                instructionAutoCompleter = TextFields.bindAutoCompletion(instructionSelector.getEditor(), instructionIds);
            }
        });
        instructionSelector.setOnShown((e) -> {
            if (instructionAutoCompleter!=null) {
                instructionAutoCompleter.dispose();
                instructionAutoCompleter = null;
            }
        });
                
        
        Button newInstructionBtn = new Button("Add");
        newInstructionBtn.setId("append-instruction-button");
        
        Consumer<Event> onAddInstructionHandler = (event) -> {
            Object selectedItem = instructionSelector.getSelectionModel().getSelectedItem();
            InstructionExpressionMeta selected;
            if (selectedItem instanceof String) {
                selected = instructionExpressionMetas.get(selectedItem);
            } else {
                selected = (InstructionExpressionMeta) selectedItem;
            }
            controller.getModel().addInstruction(selected);
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }; 
        
        newInstructionBtn.setOnAction(onAddInstructionHandler::accept);
        instructionSelector.setOnKeyReleased( e -> {
            if (ENTER.equals(e.getCode())) {
                onAddInstructionHandler.accept(e);
                e.consume();
            }
        });
        addInstructionPane.getChildren().addAll(instructionSelector, newInstructionBtn);

        TitledPane addInstructionLayer = new TitledPane();
        addInstructionLayer.setText("Add instruction");
        addInstructionLayer.setContent(addInstructionPane);
        addInstructionLayer.setExpanded(true);
        addInstructionLayer.setCollapsible(false);

        bottomPane.getChildren().add(addInstructionLayer);
    }
    
    protected List<Node> buildLayerRows(InstructionExpression instruction) {
        return instruction.getParameters().stream().map(parameter -> {
            HBox parameterPane = new HBox(5);
            parameterPane.setPrefHeight(20);
            parameterPane.setMaxHeight(20);
            parameterPane.setMinHeight(20);
            parameterPane.setPadding(new Insets(0, 0, 0, 30));
            String even = "-even";
            if (oddIndex % 2 != 0) {
                even = "-odd";
            }
            parameterPane.getStyleClass().addAll("field-row" + even);
            oddIndex++;

            FEInstructionParameterField instructionParameter = injector.getInstance(FEInstructionParameterField.class);
            instructionParameter.init(parameter);
            instructionParameter.setInstruction(instruction);
            BorderPane instructionParameterPane = new BorderPane();
            instructionParameterPane.setLeft(instructionParameter);

            BorderPane labelPane = new BorderPane();
            labelPane.setLeft(new Text(parameter.getId()));
            labelPane.getStyleClass().addAll("instruction-parameter-label-pane");

            parameterPane.getChildren().addAll(labelPane, instructionParameterPane);
            return parameterPane;
        }).collect(Collectors.toList());
    }
    
    private MenuItem addMenuItem(ContextMenu ctxMenu, String text, EventHandler<ActionEvent> action) {
        MenuItem menuItem = new MenuItem();
        menuItem.setText(text);
        menuItem.setOnAction(action);
        ctxMenu.getItems().add(menuItem);
        return menuItem;
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
//        Text instructionName = new Text(instruction.getId());
//        instructionName.setOnMouseReleased(e -> {
//            PopOver popOver = new PopOver();
//            Text help = new Text(instruction.getHelp());
//            popOver.setContentNode(help);
//            popOver.setTitle("Help - " + instruction.getId());
//            popOver.setAnimated(true);
//            popOver.show(instructionName);
//        });
//        FlowPane instructionNamePane = new FlowPane();
//        instructionNamePane.getChildren().addAll(instructionName, new Text("("));
//        grid.add(instructionNamePane, 0, rowIdx++);
//         
//        // Add parameters
//        for(FEInstructionParameter2 parameter : instruction.getParameters()) {
//            HBox parameterPane = getParameterPane();
//            
//            FEInstructionParameterField instructionField = injector.getInstance(FEInstructionParameterField.class);
//            instructionField.init(parameter);
//            instructionField.setInstruction(instruction);
//
//            parameterPane.getChildren().addAll(new Text(parameter.getId()), new Text("="), instructionField);
//            grid.add(parameterPane, 0, rowIdx++);
//        }
//        
//        grid.add(new Text(")"), 0, rowIdx++);
//        grid.add(new Text(""), 0, rowIdx++);
        return rowIdx;
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

    public void setTopPane(VBox topPane) {
        this.topPane = topPane;
    }

    public void setBottomPane(VBox bottomPane) {
        this.bottomPane = bottomPane;
    }

    public void setScrollPane(ScrollPane scrollPane) {
        this.scrollPane = scrollPane;
    }
}
