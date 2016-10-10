package com.xored.javafx.packeteditor.metatdata;

import com.google.gson.JsonElement;
import com.xored.javafx.packeteditor.data.FieldRules;
import com.xored.javafx.packeteditor.data.IField;

import java.util.List;
import java.util.Map;

public class FieldMetadata {
    
    private String id;
    private String name;
    private IField.Type type;
    private Boolean auto;
    private FieldRules fieldRules;
    private Map<String, JsonElement> dictionary;
    private List<BitFlagMetadata> bits;

    public FieldMetadata(
            String id, String name, IField.Type type,
            Map<String, JsonElement> dictionary, List<BitFlagMetadata> bits, Boolean auto) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.dictionary = dictionary;
        this.bits = bits;
        this.auto = auto;
    }

    public FieldMetadata(
            String id, String name, IField.Type type,
            Map<String, JsonElement> dictionary, List<BitFlagMetadata> bits, Boolean auto, FieldRules fieldRules) {
        this(id, name, type, dictionary, bits, auto);
        this.fieldRules = fieldRules;
    }

    /** bit definition for bit fields */
    public List<BitFlagMetadata> getBits() {
        return bits;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public IField.Type getType() {
        return type;
    }

    /** dictionary for enum fields */
    public Map<String, JsonElement> getDictionary() {
        return dictionary;
    }

    /** true if the field is automatically calculated */
    public boolean isAuto() {
        return auto != null && auto;
    }

    public FieldRules getFieldRules() {
        return fieldRules;
    }
}
