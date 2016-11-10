package com.xored.javafx.packeteditor.controls;

import com.google.gson.JsonPrimitive;
import com.xored.javafx.packeteditor.data.FEInstructionParameter;
import com.xored.javafx.packeteditor.scapy.ScapyException;
import com.xored.javafx.packeteditor.view.ComboBoxItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type.ENUM;

public class FEInstructionParameterField extends EditableField {
    
    private FEInstructionParameter feInstructionParameter;
    
    public void init(FEInstructionParameter option) {
        this.feInstructionParameter = option;
        switch (option.getType()) {
            case ENUM:
                this.editableControl = createEnumField();
                break;
            case NUMBER:
            case STRING:
            default:
                this.editableControl = createTextField(); 
        }
        this.label = createLabel();
       
        getChildren().addAll(label);
    }

    @Override
    protected String getUniqueViewId() {
        return view.getUniqueIdFor(feInstructionParameter.getCombinedField()) + "-" + feInstructionParameter.getId();
    }

    @Override
    protected void revertTextFieldValue(TextField tf) {
        tf.setText(feInstructionParameter.getValue());
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
        return getInstructionParamValue();
    }

    @Override
    protected void processDefaultAndSetItems(ComboBox<ComboBoxItem> combo, List<ComboBoxItem> items) {
        combo.getItems().addAll(items);
        Optional<ComboBoxItem> defaultComboBoxItem = items.stream()
                .filter(item -> item.getValue().getAsString().equals(feInstructionParameter.getValue()))
                .findFirst();
        if (defaultComboBoxItem.isPresent()) {
            combo.setValue(defaultComboBoxItem.get());
        }
    }
    
    @Override
    protected List<ComboBoxItem> getComboBoxItems() {
        return feInstructionParameter.getMeta().getDict().entrySet().stream()
                .map(entry -> new ComboBoxItem(entry.getValue(), new JsonPrimitive(entry.getKey())))
                .collect(Collectors.toList());
    }

    @Override
    protected TextField getTextField() {
        return new TextField(getInstructionParamValue());
    }

    private String getInstructionParamValue() {
        String value = feInstructionParameter.getValue();
        if (value == null) {
            value = feInstructionParameter.getDefaultValue();
        }
        if (ENUM.equals(feInstructionParameter.getType())) {
            String humanValue = feInstructionParameter.getMeta().getDict().get(value);
            value = String.format("%s (%s)", humanValue, value);
        }
        return value;
    }
    
    @Override
    protected void commitChanges(ComboBox<ComboBoxItem> combo) {
         controller.getModel().setVmInstructionParameter(feInstructionParameter, combo.getSelectionModel().getSelectedItem().getValue().getAsString());
    }

    @Override
    protected void commitChanges(TextField textField) {
        String prevValue = feInstructionParameter.getValue();
        try {
            controller.getModel().setVmInstructionParameter(feInstructionParameter, textField.getText());
        } catch (ScapyException e) {
            textField.getStyleClass().add("field-error");
            controller.getModel().getUserModel().setFEInstructionParameter(feInstructionParameter, prevValue);
        }
    }

    @Override
    protected String getLabelCssClass() {
        return "field-value-default";
    }
    
    @Override
    protected boolean isCheckBoxEditable() {
        return false;
    }

    @Override
    protected void onComboBoxSelectedAction(ComboBox<ComboBoxItem> combo) {
        commitChanges(combo);
    }
}
