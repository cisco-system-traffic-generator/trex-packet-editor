package com.xored.javafx.packeteditor.controls;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.xored.javafx.packeteditor.controllers.FieldEditorController;
import com.xored.javafx.packeteditor.view.ComboBoxItem;
import com.xored.javafx.packeteditor.view.FieldEditorView;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class EditableField extends FlowPane {

    @Inject
    protected Injector injector;

    @Inject
    protected FieldEditorView view;
    
    @Inject
    protected FieldEditorController controller;

    protected Node editableControl;

    protected Label label;

    protected Boolean isValid = true;
    protected boolean textChanged = false; // for text field onLostFocus
    protected boolean comboChanged = false; // for text field onLostFocus
    protected AutoCompletionBinding<String> comboAutoCompleter;

    Consumer<Void> focusControl = (v) -> editableControl.requestFocus();

    @Inject
    @Named("resources")
    protected ResourceBundle resourceBundle;

    protected Logger logger = LoggerFactory.getLogger(EditableField.class);

    protected void showControl() {
        getChildren().clear();
        getChildren().add(editableControl);
        textChanged = false;
        focusControl.accept(null);
    }

    protected void showLabel() {
        getChildren().clear();
        getChildren().add(label);
    }

    protected Label createLabel() {
        Label label = new Label(getLabelText());
        label.setId(getUniqueViewId());

        label.getStyleClass().add(getLabelCssClass());

        label.setOnMouseClicked(this::onLableClickedAction);

        return label;
    }

    protected abstract String getUniqueViewId();

    protected  Control createEnumField() {
        ComboBox<ComboBoxItem> combo = new ComboBox<>();
        combo.setId(getUniqueViewId());
        combo.setEditable(isCheckBoxEditable());
        combo.getStyleClass().addAll(getComboBoxStyles());


        focusControl = (v) -> {
            combo.getEditor().requestFocus();
            combo.getEditor().selectAll();
        };
        List<ComboBoxItem> items = getComboBoxItems();
        processDefaultAndSetItems(combo, items);
        
        combo.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // On lost focus
            if (!newValue) {
                if (comboAutoCompleter!=null) {
                    comboAutoCompleter.dispose();
                    comboAutoCompleter = null;
                }
                if(comboChanged) {
                    commitChanges(combo);
                }
                else {
                    showLabel();
                }
            }
            else {
                if (comboAutoCompleter == null) {
                    comboAutoCompleter = TextFields.bindAutoCompletion(combo.getEditor(),
                            items.stream().map(ComboBoxItem::toString).collect(Collectors.toList()));
                }
            }
        });

        combo.setOnHidden((e) -> {
            if (comboAutoCompleter == null) {
                comboAutoCompleter = TextFields.bindAutoCompletion(combo.getEditor(),
                        items.stream().map(ComboBoxItem::toString).collect(Collectors.toList()));
            }
        });

        combo.setOnShown((e) -> {
            if (comboAutoCompleter!=null) {
                comboAutoCompleter.dispose();
                comboAutoCompleter = null;
            }
        });

        combo.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            comboChanged = true;
        });

        // Update the flag when the index was changed
        combo.getSelectionModel().selectedIndexProperty().addListener((observable, oldvalue, newvalue) -> {
            comboChanged = true;
        });


        combo.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                if (comboAutoCompleter!=null) {
                    comboAutoCompleter.dispose();
                    comboAutoCompleter = null;
                }
                comboChanged = false;
                showLabel();
            }
            else if (e.getCode().equals(KeyCode.ENTER)) {
                if (comboAutoCompleter!=null) {
                    comboAutoCompleter.dispose();
                    comboAutoCompleter = null;
                }
                comboChanged = true;
                commitChanges(combo);
            }
        });

        if(!isCheckBoxEditable()) {
            combo.setOnAction((event) -> {
                onComboBoxSelectedAction(combo);
            });
        }

        return combo;
    }

    protected void onComboBoxSelectedAction(ComboBox<ComboBoxItem> combo) {}

    protected List<String> getComboBoxStyles() {
        return Arrays.asList("control", "enum-control");
    }

    protected abstract boolean isCheckBoxEditable();

    protected abstract void processDefaultAndSetItems(ComboBox<ComboBoxItem> combo, List<ComboBoxItem> items);

    protected TextField createTextField() {
        TextField tf = getTextField();
        focusControl = (v) -> {
            tf.requestFocus();
            tf.selectAll();
        };
        tf.textProperty().addListener((observable, oldValue, newValue) -> {
            textChanged = true;
        });
        tf.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // On lost focus
            if (!newValue) {
                if(textChanged && isValid()) {
                    commitChanges(tf);
                } else {
                    showLabel();
                }
            }
        });
        
        tf.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                if (isValid()) {
                    commitChanges(tf);
                    textChanged = false;
                }
            }
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                revertTextFieldValue(tf);
                textChanged = false;
                this.showLabel();
            }
        });
        return tf;
    }

    protected abstract void revertTextFieldValue(TextField tf);

    private boolean isValid() {
        return isValid;
    }

    public abstract ContextMenu getContextMenu();

    protected abstract void onLableClickedAction(MouseEvent mouseEvent);

    protected abstract String getLabelText();

    protected abstract List<ComboBoxItem> getComboBoxItems();

    protected abstract TextField getTextField();

    protected abstract void commitChanges(ComboBox<ComboBoxItem> combo);

    protected abstract void commitChanges(TextField textField);

    protected abstract String getLabelCssClass();
}
