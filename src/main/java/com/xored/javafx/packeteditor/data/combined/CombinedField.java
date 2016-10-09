package com.xored.javafx.packeteditor.data.combined;

import com.google.gson.JsonElement;
import com.xored.javafx.packeteditor.data.user.UserField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.scapy.FieldData;

import java.util.List;

public class CombinedField {
    UserField userField;

    FieldData scapyField;
    FieldMetadata meta;
    CombinedProtocol parent;

    public String getId() { return meta.getId(); }
    /**
     * use metadata to get id, name, type, ...
     */
    public FieldMetadata getMeta() {
        return meta;
    }


    public String getDisplayValue() { return getScapyDisplayValue(); }
    public JsonElement getValue() { return getScapyValue(); }

    public JsonElement getUserValue() {
        return userField != null ? userField.getValue() : null;
    }
    public String getUserStringValue() {
        return userField != null ? userField.getStringValue() : null;
    }

    /**
     * scapy model is optional. it won't be created if user model contains errors
     */
    public JsonElement getScapyValue() {
        return scapyField != null ? scapyField.getValue() : null;
    }

    public String getScapyDisplayValue() {
        return scapyField != null ? scapyField.getHumanValue() : null;
    }

    public CombinedProtocol getProtocol() {
        return parent;
    }

    public FieldData getScapyFieldData() {
        return scapyField;
    }

}
