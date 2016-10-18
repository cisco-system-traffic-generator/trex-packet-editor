package com.xored.javafx.packeteditor.controls;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.data.FieldRules;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.FieldValue;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.view.ComboBoxItem;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.BYTES;
import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.ENUM;

public class ProtocolField extends FlowPane {

    @Inject
    Injector injector;
    
    @Inject
    FieldEditorController controller;

    @Inject
    FieldEditorView view;
    
    CombinedField combinedField;
    
    Node editableControl;
    
    Label label;

    Boolean isValid = true;
    boolean textChanged = false; // for text field onLostFocus
    boolean comboChanged = false; // for text field onLostFocus

    Consumer<Void> focusControl = (v) -> {
        editableControl.requestFocus();
    };

    @Inject
    @Named("resources")
    ResourceBundle resourceBundle;

    private Logger logger = LoggerFactory.getLogger(ProtocolField.class);

    public void init(CombinedField combinedField) {
        this.combinedField = combinedField;
        this.editableControl = createControl();
        this.label = createLabel();
        
        getChildren().addAll(label);
     }

    private void showControl() {
        getChildren().clear();
        getChildren().add(editableControl);
        textChanged = false;
        focusControl.accept(null);
    }

    private void showLabel() {
        getChildren().clear();
        getChildren().add(label);
    }

    private Label createLabel() {
        String labelText = combinedField.getDisplayValue();

        if (combinedField.getMeta().getType() == ENUM) {
            // for enums also show value
            JsonElement val = combinedField.getMeta().getDictionary().getOrDefault(labelText, null);
            if (val != null) {
                labelText = String.format("%s (%s)", labelText, val.toString());
            }
        } else if (hasDefaultValue() && combinedField.getMeta().isAuto()) {
            labelText = String.format("%s (auto-calculated)", labelText);
        }

        Label label = new Label(labelText);
        label.setId(view.getUniqueIdFor(combinedField));

        String cssClassName = hasDefaultValue() ? "field-value-default" : "field-value-set";
        if (combinedField.getMeta().getType() == BYTES) {
            cssClassName = cssClassName.concat("-raw");
            label.getStyleClass().removeAll("label");
        }
        label.getStyleClass().add(cssClassName);
        
        label.setOnMouseClicked(e -> {
            showControl();
            controller.selectField(combinedField);
        });
        return label;
    }

    private Node createControl() {
        Node fieldControl;
        
        switch(combinedField.getMeta().getType()) {
            case ENUM:
                fieldControl = createEnumField();
                break;
            case BYTES:
                fieldControl = createPayloadField();
                break;
            case STRING:
            case EXPRESSION:
            default:
                fieldControl = createTextField();
        }
        return fieldControl;
    }

    private PayloadEditor createPayloadField() {
        PayloadEditor pe = new PayloadEditor(injector);
        if (combinedField.getValue() instanceof JsonPrimitive) {
            pe.setText(combinedField.getValue().getAsString());
        } else {
            pe.setText(combinedField.getScapyDisplayValue());
        }

        pe.setOnActionSave((event) -> {
            commitChanges(pe);
        });

        pe.setOnActionCancel(e -> {
            //pe.setText(combinedField.getScapyFieldData().getValue().getAsString());
            if (combinedField.getValue() instanceof JsonPrimitive) {
                pe.setText(combinedField.getValue().getAsString());
            } else {
                pe.setText(combinedField.getScapyDisplayValue());
            }
            this.showLabel();
        });

        return  pe;
    }
    
    private Control createEnumField() {
        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.setId(view.getUniqueIdFor(combinedField));
        combo.setEditable(true);
        combo.getStyleClass().addAll("control", "enum-control");
        List<ComboBoxItem> items = getComboBoxItems();


        focusControl = (v) -> {
            combo.getEditor().requestFocus();
            combo.getEditor().selectAll();
        };
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

        AutoCompletionBinding<String> stringAutoCompletionBinding = TextFields.bindAutoCompletion(combo.getEditor(), items.stream().map(ComboBoxItem::toString).collect(Collectors.toList()));

        combo.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // On lost focus
            if (!newValue) {
                stringAutoCompletionBinding.dispose();
                if(comboChanged) {
                    commitChanges(combo);
                }
                else {
                    comboChanged = false;
                    showLabel();
                }
            }
        });

        combo.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            comboChanged = true;
        });

        // Update the flag when the index was changed
        combo.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov, final Number oldvalue, final Number newvalue) {
                if (oldvalue.intValue() != newvalue.intValue()) {
                    comboChanged = true;
                }
            }
        });

        combo.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                stringAutoCompletionBinding.dispose();
                comboChanged = false;
                showLabel();
            }
            else if (e.getCode().equals(KeyCode.ENTER)) {
                stringAutoCompletionBinding.dispose();
                comboChanged = true;
                commitChanges(combo);
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
    
    private Validator<String> createTextFieldValidator(FieldMetadata fieldMetadata) {
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
        tf.setId(view.getUniqueIdFor(combinedField));
        tf.rightProperty().get().setOnMouseReleased(event -> clearFieldValue());

        if (combinedField.getValue() instanceof JsonPrimitive || isExpressionField()) {
            tf.setText(combinedField.getDisplayValue());
        }

        focusControl = (v) -> {
            tf.requestFocus();
            tf.selectAll();
        };

        tf.setContextMenu(getContextMenu());
        createValidator(tf);

        tf.textProperty().addListener((observable, oldValue, newValue) -> {
            textChanged = true;
        });
        tf.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // On lost focus
            if (!newValue) {
                if(textChanged) {
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
                textChanged = false;
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
        });
    }

    private void clearFieldValue() {
        setModelValue(ReconstructField.resetValue(combinedField.getId()), null);
    }

    private void randomizeFieldValue() {
        setModelValue(ReconstructField.randomizeValue(combinedField.getId()), null);
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

    private void commitChanges(ComboBox combo) {
        List<ComboBoxItem> items = getComboBoxItems();
        Object sel = combo.getSelectionModel().getSelectedItem(); // yes, it can be string
        ReconstructField newVal;
        if (sel instanceof String) {
            ComboBoxItem item = items.stream().filter(f -> f.toString().equals(sel)).findFirst().orElse(null);
            if (item != null) {
                // selected item from list. (enums use strings to pass values)
                newVal = ReconstructField.setValue(combinedField.getId(), item.getValue().getAsString());
            } else {
                // raw string value
                if ("".equals(sel)) {
                    newVal = ReconstructField.resetValue(combinedField.getId());
                } else {
                    newVal = ReconstructField.setValue(combinedField.getId(), (String)sel);
                }
            }
        } else if (sel instanceof ComboBoxItem) {
            newVal = ReconstructField.setValue(combinedField.getId(), ((ComboBoxItem)sel).getValue().getAsString());
        } else {
            logger.warn("Ignored input on {}", combinedField.getId());
            return;
        }
        setModelValue(newVal, combo);
    }

    private boolean isExpressionField() {
        FieldData fd = combinedField.getScapyFieldData();
        return FieldMetadata.FieldType.EXPRESSION.equals(combinedField.getMeta().getType()) ||
                fd != null && FieldValue.ObjectType.EXPRESSION.equals(fd.getObjectValueType());

    }
    private void commitChanges(TextField textField) {
        textChanged = false;
        if (isExpressionField()) {
            setModelValue(ReconstructField.setExpressionValue(combinedField.getId(), textField.getText()), textField);
        } else {
            setModelValue(ReconstructField.setHumanValue(combinedField.getId(), textField.getText()), textField);
        }
    }

    private void commitChanges(PayloadEditor payloadEditor) {
        PayloadEditor.PayloadType type = payloadEditor.getType();

        if (type == PayloadEditor.PayloadType.TEXT
                || type == PayloadEditor.PayloadType.TEXT_PATTERN) {
            setModelValue(ReconstructField.setHumanValue(combinedField.getId(), payloadEditor.getText()), null);
        }
        else {
            byte[] data = payloadEditor.getData();
            setModelValue(ReconstructField.setValue(combinedField.getId(), data), null);
        }
    }

    private void setModelValue(ReconstructField modify, Node valueNode) {
        try {
            logger.info("Committing changes to {}", view.getUniqueIdFor(combinedField));
            controller.getModel().editField(combinedField, modify);
        } catch (Exception e) {
            logger.warn("Failed to build packet with new value of {}", combinedField.getId());
            // TODO: implement validator and/or message box/popup
            if (valueNode != null) {
                valueNode.getStyleClass().add("field-error");
            }
        }
    }

    private boolean isValid() {
        return isValid;
    }

}
