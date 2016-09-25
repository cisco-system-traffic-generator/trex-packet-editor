package com.xored.javafx.packeteditor.events;

import com.xored.javafx.packeteditor.data.IField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;

public class FieldEvent extends Event{
    
    private Action action;

    private FieldMetadata fieldMetadata;

    private Object value;

    public FieldEvent(Action action, IField field) {
        this.action = action;
        this.fieldMetadata = field.getMeta();
    }

    public Action getAction() {
        return action;
    }
    public Object getValue() {
        return value;
    }

    public FieldMetadata getFieldMetadata() {
        return fieldMetadata;
    }
}
