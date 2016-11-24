package com.xored.javafx.packeteditor.controls;

import com.google.common.base.Strings;
import com.google.gson.JsonPrimitive;
import com.xored.javafx.packeteditor.data.FeParameter;
import com.xored.javafx.packeteditor.metatdata.FeParameterMeta;
import com.xored.javafx.packeteditor.scapy.ScapyException;
import com.xored.javafx.packeteditor.view.ComboBoxItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FeParameterField extends EditableField {

    private FeParameter feParameter;
    
    public void init(FeParameter feParameter) {
        this.feParameter = feParameter;
        this.editableControl = FeParameterMeta.FeParameterType.STRING.equals(feParameter.getType()) ? createTextField() : createEnumField();
        this.label = createLabel();

        getChildren().addAll(label);
    }
    
    @Override
    protected String getUniqueViewId() {
        return  "fe-parameter-" + feParameter.getId();
    }

    @Override
    protected boolean isCheckBoxEditable() {
        return false;
    }

    @Override
    protected void processDefaultAndSetItems(ComboBox<ComboBoxItem> combo, List<ComboBoxItem> items) {
        combo.getItems().addAll(items);
        ComboBoxItem defaultItem = getNotSelectedItem();
        
        if (!Strings.isNullOrEmpty(feParameter.getValue())) {
            Optional<ComboBoxItem> selected = items.stream().filter(item -> feParameter.getValue().equals(item.getValue().getAsString())).findFirst();
            if(selected.isPresent()) {
                defaultItem = selected.get();
            }
        }
        combo.setValue(defaultItem);
    }

    @Override
    protected void revertTextFieldValue(TextField tf) {
        tf.setText(feParameter.getValue());
    }

    @Override
    public ContextMenu getContextMenu() {
        return null;
    }

    @Override
    protected void onLableClickedAction(MouseEvent mouseEvent) {
        if (!MouseButton.SECONDARY.equals(mouseEvent.getButton())) {
            showControl();
        }
    }

    @Override
    protected String getLabelText() {
        return Strings.isNullOrEmpty(feParameter.getValue()) ? "Not Selected" : feParameter.getValue();
    }

    @Override
    protected List<ComboBoxItem> getComboBoxItems() {
        List<ComboBoxItem> items = new ArrayList<>();
        items.add(getNotSelectedItem());
        
        return items;
    }

    private ComboBoxItem getNotSelectedItem() {
        return new ComboBoxItem("Not Selected", new JsonPrimitive(""));
    }
    
    @Override
    protected TextField getTextField() {
        TextField textField = new TextField(feParameter.getValue());
        textField.getStyleClass().add("text-field-100");
        return textField;
    }

    @Override
    protected void commitChanges(ComboBox<ComboBoxItem> combo) {
        String selection = combo.getSelectionModel().getSelectedItem().getValue().getAsString();
        controller.getModel().setFeParameterValue(feParameter.getId(), selection);
    }

    @Override
    protected void commitChanges(TextField textField) {
        String prevValue = feParameter.getValue();
        try {
            controller.getModel().setFeParameterValue(feParameter.getId(), textField.getText());
        } catch (ScapyException e) {
            textField.getStyleClass().add("field-error");
            controller.getModel().setFeParameterValue(feParameter.getId(), prevValue);
        }
    }

    @Override
    protected String getLabelCssClass() {
        return "field-value-default";
    }

    protected List<String> getComboBoxStyles() {
        List<String> parentStyles = super.getComboBoxStyles();
        List<String> feParametersStyles = new ArrayList<>(parentStyles);
        feParametersStyles.add("combo-box-100");
        return feParametersStyles;
    }

    @Override
    protected void onComboBoxSelectedAction(ComboBox<ComboBoxItem> combo) {
        commitChanges(combo);
    }
}
