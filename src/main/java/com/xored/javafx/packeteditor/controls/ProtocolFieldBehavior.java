package com.xored.javafx.packeteditor.controls;

import com.sun.javafx.scene.control.behavior.TextFieldBehavior;
import javafx.css.PseudoClass;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class ProtocolFieldBehavior extends TextFieldBehavior {

    private ProtocolField protocolField;
    private Boolean focusTraversable;

    /************************************************************************
     * *
     * \defgroup Constructors                                               *
     * Constructors and helper methods for constructors                     *
     * *
     *
     * @{ *
     ***********************************************************************/

    public ProtocolFieldBehavior(final ProtocolField protocolField) {
        super(protocolField);
        this.protocolField = protocolField;
        init();
    }

    private void init() {
        focusTraversable = false;

        protocolField.setOnMouseClicked(this::handleMouseClicked);
        protocolField.setOnKeyPressed(this::handleKeyPressed);
        protocolField.focusedProperty().addListener((observable, oldValue, newValue) -> handleFocusChange(newValue));
        protocolField.focusTraversableProperty().addListener((observable, oldValue, newValue) -> handleFocusTraversableChange(newValue));
    }

    private void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER:
                protocolField.setValue(protocolField.getText());
                exitEditableMode();
                break;
            case ESCAPE:
                exitEditableMode();
                break;
        }
    }

    private void handleMouseClicked(MouseEvent event) {
        if (event.getClickCount() == protocolField.getEditableClicks() && !this.isEditing()) {
            enterEditableMode();
        }
    }

    private void handleFocusChange(Boolean newValue) {
        if (!newValue) {
            // Save changes and exit editable mode
            protocolField.setValue(protocolField.getText());
            exitEditableMode();
        } else if (focusTraversable) {
            enterEditableMode();
        }
    }

    private void handleFocusTraversableChange(Boolean newValue) {
        focusTraversable = newValue;
    }

    private void enterEditableMode() {
        protocolField.setEditable(true);
        protocolField.deselect();
        protocolField.pseudoClassStateChanged(PseudoClass.getPseudoClass("editable"), true);
    }

    private void exitEditableMode() {
        protocolField.setEditable(false);
        protocolField.deselect();
        protocolField.pseudoClassStateChanged(PseudoClass.getPseudoClass("editable"), false);
    }

}