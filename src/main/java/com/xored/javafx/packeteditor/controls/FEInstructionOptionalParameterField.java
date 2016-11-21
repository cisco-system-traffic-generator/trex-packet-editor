package com.xored.javafx.packeteditor.controls;

import com.google.gson.JsonPrimitive;
import com.xored.javafx.packeteditor.view.ComboBoxItem;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.List;
import java.util.stream.Collectors;

public class FEInstructionOptionalParameterField extends FEInstructionParameterField {

    protected Node createLabel() {
        Text valueNode = new Text("more");
        valueNode.setFill(Color.web("grey"));
        valueNode.setOnMouseClicked(this::onLableClickedAction);
        return valueNode;
    }
    
    @Override
    protected String getUniqueViewId() {
        return feInstructionParameter.getId();
    }

    @Override
    protected void revertTextFieldValue(TextField tf) {
        tf.setText("more");
    }

    @Override
    public ContextMenu getContextMenu() {
        return null;
    }

    @Override
    protected void onLableClickedAction(MouseEvent event) {
        if (!MouseButton.SECONDARY.equals(event.getButton())) {
            showControl();
        }
    }

    @Override
    protected String getLabelText() {
        return "more";
    }

    @Override
    protected void processDefaultAndSetItems(ComboBox<ComboBoxItem> combo, List<ComboBoxItem> items) {
        combo.getItems().addAll(items);
    }
    
    @Override
    protected List<ComboBoxItem> getComboBoxItems() {
        return feInstructionParameter.getMeta().getDict().entrySet().stream()
                .map(entry -> new ComboBoxItem(entry.getValue(), new JsonPrimitive(entry.getKey())))
                .collect(Collectors.toList());
    }

    @Override
    protected TextField getTextField() { return null; }

    @Override
    protected void commitChanges(ComboBox<ComboBoxItem> combo) {}

    @Override
    protected boolean isCheckBoxEditable() {
        return false;
    }
    
}
