package com.xored.javafx.packeteditor.controls;

import com.google.common.base.Strings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;

/**
 * A TextField, that implements some Label functionality
 * <p>
 * It acts as a Label, by removing the TextField style and making it non-editable.
 * It is also not focus traversable.
 * <p>
 * When clicking on it, it will switch to editable mode
 * Changing focus away from the ProtocolField or pressing ENTER will save the changes made and deactivate editable mode.
 * When pressing ESC it will exit editable mode without saving the changes made.
 *
 */
public class ProtocolField extends TextField {

    private StringProperty value;

    /**
     * Clicks needed to enter editable-mode
     */
    private IntegerProperty editableClicks;

    public ProtocolField() {
        this("");
    }

    public ProtocolField(String text) {
        super(text);
        getStyleClass().setAll("editable-label");
        init();
    }

    private void init() {
        editableClicks = new SimpleIntegerProperty(1);
        value = new SimpleStringProperty(getText());
        setFocusTraversable(false);
        setEditable(true);
        setEditableClicks(1);
    }
    
    @Override
    protected Skin<?> createDefaultSkin() { return new ProtocolFieldSkin(this); }

    public int getEditableClicks() {
        return editableClicks.get();
    }

    public IntegerProperty editableClicksProperty() {
        return editableClicks;
    }

    public void setEditableClicks(int editableClicks) {
        this.editableClicks.set(editableClicks);
    }

    public String getValue() {
        return value.get();
    }

    public StringProperty valueProperty() {
        return value;
    }

    public void setValue(String baseText) {
        this.value.set(Strings.isNullOrEmpty(baseText)? "Not set" : baseText);
    }
}