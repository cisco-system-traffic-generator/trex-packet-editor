package com.xored.javafx.packeteditor.controls;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.data.FieldRules;
import com.xored.javafx.packeteditor.data.IField;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.view.ComboBoxItem;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProtocolField extends FlowPane {
    
    @Inject
    FieldEditorController controller;

    @Inject
    FieldEditorView view;
    
    CombinedField combinedField;
    
    Node editableControl;
    
    Label label;

    Boolean isValid = true;
    
    @Inject
    @Named("resources")
    ResourceBundle resourceBundle;

    public void init(CombinedField combinedField) {
        this.combinedField = combinedField;
        this.editableControl = createControl();
        this.label = createLabel();
        
        getChildren().addAll(label);
     }

    private void showControl() {
        getChildren().clear();
        getChildren().add(editableControl);
        editableControl.requestFocus();
    }

    private void showLabel() {
        getChildren().clear();
        getChildren().add(label);
    }

    private Label createLabel() {
        String labelText = combinedField.getDisplayValue();

        if (combinedField.getMeta().getType() == IField.Type.ENUM) {
            // for enums also show value
            JsonElement val = combinedField.getMeta().getDictionary().getOrDefault(labelText, null);
            if (val != null) {
                labelText = String.format("%s (%s)", labelText, val.toString());
            }
        } else if (hasDefaultValue() && combinedField.getMeta().isAuto()) {
            labelText = String.format("%s (auto-calculated)", labelText);
        }

        Label label = new Label(labelText);

        String cssClassName = hasDefaultValue() ? "field-value-default" : "field-value-set";
        label.getStyleClass().add(cssClassName);
        
        label.setOnMouseClicked(e -> {
            if (!view.hasInvalidInput()) {
                showControl();
                controller.selectField(combinedField);
            }
        });
        return label;
    }

    private Node createControl() {
        Node fieldControl;
        
        switch(combinedField.getMeta().getType()) {
            case RAW:
                throw new UnsupportedClassVersionError("Raw field types should created via FieldEditorView.createPayloadEditorControl");
            case ENUM:
                fieldControl = createEnumField();
                break;
            case MAC_ADDRESS:
            case IPV4ADDRESS:
            case TCP_OPTIONS:
            case NUMBER:
            case STRING:
            case NONE:
            default:
                fieldControl = createTextField();
        }
        return fieldControl;
    }

    private Control createEnumField() {
        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.getStyleClass().addAll("control");
        List<ComboBoxItem> items = getComboBoxItems();

        ComboBoxItem defaultValue = items.stream().filter(item ->
                        item.equalsTo(combinedField.getValue())
        ).findFirst().orElse(null);

        if (defaultValue == null && combinedField.getScapyFieldData() != null) {
            defaultValue = createDefaultCBItem(combinedField);
            items.add(defaultValue);
        }
        combo.getItems().addAll(items);
        if (defaultValue != null) {
            combo.setValue(defaultValue);
        }

        TextFields.bindAutoCompletion(combo.getEditor(), items.stream().map(ComboBoxItem::toString).collect(Collectors.toList()));

        combo.setOnAction((event) -> commitChanges(combo));

        combo.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                getChildren().clear();
                combo.setValue(createDefaultCBItem(combinedField));
                label.setText(combinedField.getScapyDisplayValue());
                getChildren().add(label);
            }
        });
        return combo;
    }

    private List<ComboBoxItem> getComboBoxItems() {
        return combinedField.getMeta().getDictionary().entrySet().stream()
                .sorted((e1, e2)->e1.getKey().compareTo(e2.getKey()))
                .map(entry -> new ComboBoxItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private ComboBoxItem createDefaultCBItem(CombinedField field) {
        ComboBoxItem defaultValue;
        FieldData fd = field.getScapyFieldData();
        defaultValue = new ComboBoxItem(fd.getHumanValue(), fd.value);
        return defaultValue;
    }
    
    private Validator createTextFieldValidator(FieldMetadata fieldMetadata) {
        FieldRules rules = fieldMetadata.getFieldRules();

        if (rules != null) {
            if(rules.hasSpecifiedInterval()) {
                return Validator.<String>createPredicateValidator(newStringValue -> {
                    try {
                        Integer newValue = Strings.isNullOrEmpty(newStringValue) ? 0 : Integer.valueOf(newStringValue);
                        return newValue >= rules.getMin() && newValue <= rules.getMax();
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }, String.format("Must be between %s and %s", rules.getMin(), rules.getMax()));
            } else if (rules.hasRegex()) {
                return Validator.createRegexValidator("", rules.getRegex(), Severity.ERROR);
            }
        }

        // An empty validator
        return Validator.createPredicateValidator(newValue -> true, "");
    }
    
    private TextField createTextField() {
        CustomTextField tf = (CustomTextField) TextFields.createClearableTextField();
        tf.rightProperty().get().setOnMouseReleased(event -> clearFieldValue());

        if (combinedField.getValue() instanceof JsonPrimitive) {
            tf.setText(combinedField.getValue().getAsString());
        }

        tf.setContextMenu(getContextMenu());
        createValidator(tf);
                
        tf.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // On lost focus
            if (!newValue) {
                if(hasChanged(tf)) {
                    if (isValid()) {
                        commitChanges(tf);
                    } else {
                        tf.requestFocus();
                    }
                } else {
                    showLabel();
                }
            }
        });
        
        tf.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                if (isValid()) {
                    commitChanges(tf);
                }
            }
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                tf.setText(combinedField.getScapyDisplayValue());
                this.showLabel();
            }
        });
        return tf;
    }

    private void createValidator(CustomTextField tf) {
        ValidationSupport validationSupport = new ValidationSupport();
        validationSupport.setValidationDecorator(new StyleClassValidationDecoration("field-error", "field-warning"));
        validationSupport.registerValidator(tf, createTextFieldValidator(combinedField.getMeta()));
        validationSupport.invalidProperty().addListener((observable, oldValue, newValue) -> {
            isValid = !newValue;
            view.setHasInvalidInput(newValue);
        });
    }

    private void clearFieldValue() {
        controller.getModel().editField(combinedField, ReconstructField.resetValue(combinedField.getMeta().getId()));
    }

    private void randomizeFieldValue() {
        controller.getModel().editField(combinedField, ReconstructField.randomizeValue(combinedField.getMeta().getId()));
    }

    private ContextMenu getContextMenu() {
        ContextMenu context = new ContextMenu();

        MenuItem generateItem = new MenuItem(resourceBundle.getString("GENERATE"));
        generateItem.setOnAction(event ->randomizeFieldValue());

        MenuItem defaultItem = new MenuItem(resourceBundle.getString("SET_DEFAULT"));
        defaultItem.setOnAction(event ->clearFieldValue());

        context.getItems().addAll(generateItem, defaultItem);

        return context;
    }
    
    private boolean hasDefaultValue() {
        return !controller.getModel().isBinaryMode() && !combinedField.hasUserValue();
    }

    private boolean hasChanged(TextField textField) {
        return !textField.getText().equals(combinedField.getDisplayValue());
    }

    private void commitChanges(ComboBox combo) {
        List<ComboBoxItem> items = getComboBoxItems();
        Object sel = combo.getSelectionModel().getSelectedItem(); // yes, it can be string
        if (sel instanceof String) {
            ComboBoxItem item = items.stream().filter(f -> f.toString().equals(sel)).findFirst().orElse(null);
            if (item != null) {
                // selected item from list
                controller.getModel().editField(combinedField, ReconstructField.setValue(combinedField.getId(), item.getValue().getAsString()));
            } else {
                // raw string value
                controller.getModel().editField(combinedField, ReconstructField.setValue(combinedField.getId(), (String) sel));
            }
        } else if (sel instanceof ComboBoxItem) {
            controller.getModel().editField(combinedField, ReconstructField.setValue(combinedField.getId(), ((ComboBoxItem)sel).getValue().getAsString()));
        }
    }
    
    private void commitChanges(TextField textField) {
        controller.getModel().editField(combinedField, ReconstructField.setHumanValue(combinedField.getId(), textField.getText()));
    }

    private boolean isValid() {
        return isValid;
    }
}
