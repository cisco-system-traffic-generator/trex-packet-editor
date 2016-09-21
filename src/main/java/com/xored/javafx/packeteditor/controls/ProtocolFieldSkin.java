package com.xored.javafx.packeteditor.controls;

import com.sun.javafx.scene.control.skin.TextFieldSkin;
import javafx.application.Platform;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.text.Text;

/**
 * The Skin Class for ProtocolField
 */
public class ProtocolFieldSkin extends TextFieldSkin {

    private ProtocolField protocolField;
    private Boolean editableState;
    
    public ProtocolFieldSkin(final ProtocolField protocolField) {
        this(protocolField, new ProtocolFieldBehavior(protocolField));
    }

    public ProtocolFieldSkin(final ProtocolField protocolField, final ProtocolFieldBehavior protocolFieldBehavior) {
        super(protocolField, protocolFieldBehavior);
        this.protocolField = protocolField;
        init();
    }

    private void init() {
        editableState = false;

        Platform.runLater(this::updateVisibleText);

        // Register listeners and binds
        protocolField.getPseudoClassStates().addListener((SetChangeListener<PseudoClass>) e -> {
            if (e.getSet().contains(PseudoClass.getPseudoClass("editable"))) {
                if (!editableState) {
                    // editableState change to editable
                    editableState = true;
                    updateVisibleText();
                }
            } else {
                if (editableState) {
                    // editableState change to not editable
                    editableState = false;
                    updateVisibleText();
                }
            }
        });
        protocolField.widthProperty().addListener(observable -> updateVisibleText());
        protocolField.valueProperty().addListener(observable -> updateVisibleText());
    }

    /**
     * Updates the visual text using the baseText
     */
    private void updateVisibleText() {
        String baseText = protocolField.getValue();
        if (!editableState) {
            protocolField.setText(calculateClipString(baseText));
        } else {
            protocolField.setText(baseText);
            protocolField.positionCaret(baseText.length());
        }
    }

    /**
     * Truncates text to fit into the ProtocolField
     *
     * @param text The text that needs to be truncated
     * @return The truncated text with an appended "..."
     */
    private String calculateClipString(String text) {
        double labelWidth = protocolField.getWidth();

        Text layoutText = new Text(text);
        layoutText.setFont(protocolField.getFont());

        if (layoutText.getLayoutBounds().getWidth() < labelWidth) {
            return text;
        } else {
            layoutText.setText(text + "...");
            while (layoutText.getLayoutBounds().getWidth() > labelWidth) {
                text = text.substring(0, text.length() - 1);
                layoutText.setText(text + "...");
            }
            return text + "...";
        }
    }
}