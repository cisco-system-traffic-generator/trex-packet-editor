package com.xored.javafx.packeteditor.controls;

import com.google.inject.Injector;
import com.xored.javafx.packeteditor.TRexPacketCraftingTool;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.LoggerFactory;

/**
 * Created by igor on 10/10/16.
 */
public class PayloadEditor extends VBox {
    static org.slf4j.Logger logger = LoggerFactory.getLogger(TRexPacketCraftingTool.class);

    @FXML private VBox root;
    @FXML private HBox payloadEditorHboxLabel;
    @FXML private HBox payloadEditorHboxChoice;
    @FXML private HBox payloadEditorHboxValue;

    // Label
    @FXML private Label     payloadEditorLabel;

    // Choice
    @FXML private ChoiceBox payloadChoiceType;
    @FXML private Button payloadButtonGo;
    @FXML private GridPane payloadEditorGrid;

    // Payload from text
    @FXML private TextArea textText;

    // Payload from file
    @FXML private TextField textFilename;
    @FXML private Button    textFilenameButton;

    // Payload generation from text pattern
    @FXML private TextField textPatternSize;
    @FXML private TextArea  textPatternText;

    // Payload generation from file pattern
    @FXML private TextField filePatternSize;
    @FXML private TextField filePatternFilename;
    @FXML private Button    filePatternButton;

    // Payload random ascii generation
    @FXML private TextField randomAsciiSize;

    // Payload random non-ascii generation
    @FXML private TextField randomNonAsciiSize;

    private FXMLLoader fxmlLoader = null;
    private Injector injector;

    public PayloadEditor(Injector injector) {
        this.injector = injector;
        this.fxmlLoader = injector.getInstance(FXMLLoader.class);

        fxmlLoader.setLocation(ClassLoader.getSystemResource("com/xored/javafx/packeteditor/controllers/PayloadEditor.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        payloadChoiceType.setOnAction((event) -> {
            int index = payloadChoiceType.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                payloadEditorHboxValue.setVisible(true);
                payloadEditorHboxValue.setManaged(true);
                javafx.application.Platform.runLater(() -> gridSetVisible(payloadEditorGrid, index));
            }
        });

        payloadEditorLabel.setOnMouseClicked(e -> {
            payloadEditorHboxLabel.setVisible(false);
            payloadEditorHboxLabel.setManaged(false);
            payloadEditorHboxChoice.setVisible(true);
            payloadEditorHboxChoice.setManaged(true);

            int index = payloadChoiceType.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                payloadEditorHboxValue.setVisible(true);
                payloadEditorHboxValue.setManaged(true);
            }
            else {
                payloadEditorHboxValue.setVisible(false);
                payloadEditorHboxValue.setManaged(false);
            }
        });
    }

    public String getText() {
        return textProperty().get();
    }

    public void setLabel(String value) {
        payloadEditorLabel.setText(value);
    }

    public void setText(String value) {
        textProperty().set(value);
    }

    public StringProperty textProperty() {
        return textText.textProperty();
    }

    public void setPrefSize(double width, double height) {
        super.setPrefSize(width, height);
    }

    public void setContextMenu(ContextMenu contextMenu) {
        textText.setContextMenu(contextMenu);
    }

    public SingleSelectionModel getSelectionModel() {
        return payloadChoiceType.getSelectionModel();
    }

    public void select(int index) {
        javafx.application.Platform.runLater(() -> {
            gridSetVisible(payloadEditorGrid, index);
            getSelectionModel().select(index);
        });
    }

    public final void setOnAction(EventHandler<ActionEvent> value) {
        payloadButtonGo.setOnAction(value);
    }

    private void gridSetVisible(GridPane grid, int index) {
        for (Node node : grid.getChildren()) {
            node.setVisible(false);
            node.setManaged(false);
        }
        if (index >= 0) {
            Node node = grid.getChildren().get(index);
            node.setVisible(true);
            node.setManaged(true);

            double width = node.getLayoutBounds().getWidth();
            double height = node.getLayoutBounds().getHeight();
            Pane parentpane = (Pane) grid.getParent();
            parentpane.setMinSize(width, height);
            parentpane.setPrefSize(width, height);
            parentpane.setMaxSize(width, height);
        }
    }

}
