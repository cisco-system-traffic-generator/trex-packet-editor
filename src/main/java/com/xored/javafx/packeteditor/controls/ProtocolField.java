package com.xored.javafx.packeteditor.controls;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.xored.javafx.packeteditor.data.FieldRules;
import com.xored.javafx.packeteditor.data.combined.CombinedField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.scapy.ConnectionException;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.FieldValue;
import com.xored.javafx.packeteditor.scapy.ReconstructField;
import com.xored.javafx.packeteditor.view.ComboBoxItem;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;

import java.util.List;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.BYTES;
import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.ENUM;

public class ProtocolField extends EditableField {

    private CombinedField combinedField;
    private boolean readOnlyMode;

    public void init(CombinedField combinedField, boolean readOnlyMode) {
        this.combinedField = combinedField;
        this.editableControl = createControl();
        this.label = createLabel();
        this.readOnlyMode = readOnlyMode;
       
        getChildren().addAll(label);
    }

    protected  Node createControl() {
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
    
    @Override
    protected void onLableClickedAction(MouseEvent event) {
        if (!readOnlyMode && !MouseButton.SECONDARY.equals(event.getButton())) {
            showControl();
            controller.selectField(combinedField);
        }
    }

    @Override
    protected String getLabelText() {
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
        return labelText;
    }

    @Override
    protected void processDefaultAndSetItems(ComboBox<ComboBoxItem> combo, List<ComboBoxItem> items) {
        ComboBoxItem defaultValue = items.stream().filter(item ->
                        item.equalsTo(combinedField.getValue())
        ).findFirst().orElse(null);

        if (defaultValue == null && combinedField.getScapyFieldData() != null) {
            defaultValue = createDefaultCBItem();
            items.add(defaultValue);
        }
        combo.getItems().addAll(items);
        if (defaultValue != null) {
            combo.setValue(defaultValue);
        }
    }


    @Override
    protected List<ComboBoxItem> getComboBoxItems() {
        return combinedField.getMeta().getDictionary().entrySet().stream()
                .sorted((e1, e2)->e1.getKey().compareTo(e2.getKey()))
                .map(entry -> new ComboBoxItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    protected ComboBoxItem createDefaultCBItem() {
        FieldData fd = combinedField.getScapyFieldData();
        return new ComboBoxItem(fd.getHumanValue(), fd.value);
    }

    @Override
    protected TextField getTextField() {
        CustomTextField tf = (CustomTextField) TextFields.createClearableTextField();

        tf.setId(view.getUniqueIdFor(combinedField));
        tf.rightProperty().get().setOnMouseReleased(event -> clearFieldValue());

        if (combinedField.getValue() instanceof JsonPrimitive || isExpressionField()) {
            tf.setText(combinedField.getDisplayValue());
        }
        
        tf.setContextMenu(getContextMenu());
        createValidator(tf);
        return tf;
    }

    protected void createValidator(CustomTextField tf) {
        ValidationSupport validationSupport = new ValidationSupport();
        validationSupport.setValidationDecorator(new StyleClassValidationDecoration("field-error", "field-warning"));
        validationSupport.registerValidator(tf, createTextFieldValidator(combinedField.getMeta()));
        validationSupport.invalidProperty().addListener((observable, oldValue, newValue) -> {
            isValid = !newValue;
        });
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
    
    @Override
    protected String getLabelCssClass() {
        String cssClassName = hasDefaultValue() ? "field-value-default" : "field-value-set";
        if (combinedField.getMeta().getType() == BYTES) {
            cssClassName = cssClassName.concat("-raw");
            if (label != null) {
                label.getStyleClass().removeAll("label");
            }
        }
        return cssClassName;
    }

    private boolean hasDefaultValue() {
        return !controller.getModel().isBinaryMode() && !combinedField.hasUserValue();
    }

    @Override
    protected String getUniqueViewId() {
        return view.getUniqueIdFor(combinedField);
    }

    @Override
    protected void revertTextFieldValue(TextField tf) {
        tf.setText(combinedField.getScapyDisplayValue());
    }

    private ContextMenu contextMenu;

    @Override
    public ContextMenu getContextMenu() {
        if (contextMenu != null) {
            return contextMenu;
        }

        ContextMenu context = new ContextMenu();

        MenuItem generateItem = new MenuItem(resourceBundle.getString("GENERATE"));
        generateItem.setOnAction(event ->randomizeFieldValue());

        MenuItem defaultItem = new MenuItem(resourceBundle.getString("SET_DEFAULT"));
        defaultItem.setOnAction(event ->clearFieldValue());

        Menu feInstructionsTemplatesItem = new Menu(resourceBundle.getString("FE_TEMPLATES"));
        controller.getMetadataService().getFeInstructionsTemplates().stream().forEach(template -> {
            MenuItem instructionsTemplate = new MenuItem(template.getName());
            instructionsTemplate.setOnAction(e -> controller.getModel().addFEInstructionsTemplate(combinedField, template));
            feInstructionsTemplatesItem.getItems().add(instructionsTemplate);
        });

        context.getItems().addAll(generateItem, defaultItem,feInstructionsTemplatesItem);
        context.getStyleClass().addAll("pcapEditorTopPane-context-menu");

        this.contextMenu = context;
        return context;
    }

    private void randomizeFieldValue() {
        setModelValue(ReconstructField.randomizeValue(combinedField.getId()), null);
    }

    private void setModelValue(ReconstructField modify, Node valueNode) {
        try {
            logger.info("Committing changes to {}", view.getUniqueIdFor(combinedField));
            controller.getModel().editField(combinedField, modify);
        } catch (Exception e) {
            logger.warn("Failed to build packet with new value of {}", combinedField.getId());
            if (e instanceof ConnectionException) {
                controller.showConnectionErrorDialog();
                logger.error("Connection exception occurred");
            }
            // TODO: implement validator and/or message box/popup            
            if (valueNode != null) {
                valueNode.getStyleClass().add("field-error");
                if (valueNode instanceof PayloadEditor) {
                    ((PayloadEditor) valueNode).accessibleHelpProperty().setValue("ERROR: " + e.getMessage());
                }
            }
        }
    }

    private void clearFieldValue() {
        setModelValue(ReconstructField.resetValue(combinedField.getId()), null);
    }

    @Override
    protected boolean isCheckBoxEditable() {
        return true;
    }
    
    @Override
    protected void commitChanges(ComboBox<ComboBoxItem> combo) {
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
    
    @Override
    protected void commitChanges(TextField textField) {
        textChanged = false;
        if (isExpressionField()) {
            setModelValue(ReconstructField.setExpressionValue(combinedField.getId(), textField.getText()), textField);
        } else {
            setModelValue(ReconstructField.setHumanValue(combinedField.getId(), textField.getText()), textField);
        }
    }

    private void commitChanges(PayloadEditor payloadEditor) {
        JsonElement json = payloadEditor.getJson();
        setModelValue(ReconstructField.setRawValue(combinedField.getId(), json), payloadEditor);
    }
    
    private boolean isExpressionField() {
        FieldData fd = combinedField.getScapyFieldData();
        return FieldMetadata.FieldType.EXPRESSION.equals(combinedField.getMeta().getType()) ||
                fd != null && FieldValue.ObjectType.EXPRESSION.equals(fd.getObjectValueType());
    }

    private PayloadEditor createPayloadField() {
        PayloadEditor pe = new PayloadEditor(injector);
        if (combinedField.getValue() instanceof JsonPrimitive) {
            pe.setText(combinedField.getValue().getAsString());
        }
        else if (combinedField.getUserValue() instanceof JsonPrimitive) {
            pe.setText(combinedField.getUserValue().getAsString());
        }
        else if (combinedField.getUserValue() instanceof JsonElement) {
            pe.setJson(combinedField.getUserValue());
        }
        else {
            pe.setText(combinedField.getScapyDisplayValue());
            commitChanges(pe);
        }

        pe.setOnActionSave((event) -> {
            commitChanges(pe);
        });

        pe.setOnActionCancel(e -> {
            pe.reset();
            this.showLabel();
        });

        return  pe;
    }

}
