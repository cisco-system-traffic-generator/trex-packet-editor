package com.xored.javafx.packeteditor.controls;

import com.google.gson.JsonPrimitive;
import com.xored.javafx.packeteditor.data.FEInstructionParameter;
import com.xored.javafx.packeteditor.view.ComboBoxItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta.Type.STRING;

public class FEInstructionParameterField extends EditableField {
    
    private FEInstructionParameter feInstructionParameter;
    
    public void init(FEInstructionParameter option) {
        this.feInstructionParameter = option;
        this.editableControl = STRING.equals(option.getType()) ? createTextField() : createEnumField();
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
    protected ContextMenu getContextMenu() {
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
        return value;
    }
    
    @Override
    protected void commitChanges(ComboBox<ComboBoxItem> combo) {
         controller.getModel().setVmInstructionParameter(feInstructionParameter, combo.getSelectionModel().getSelectedItem().getValue().getAsString());
    }

    @Override
    protected void commitChanges(TextField textField) {
        controller.getModel().setVmInstructionParameter(feInstructionParameter, textField.getText());
    }

    @Override
    protected String getLabelCssClass() {
        return "field-value-default";
    }
    
    @Override
    protected boolean isCheckBoxEditable() {
        return false;
    }
}
