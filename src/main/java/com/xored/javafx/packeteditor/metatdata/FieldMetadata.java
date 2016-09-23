package com.xored.javafx.packeteditor.metatdata;

import com.google.gson.JsonElement;
import com.xored.javafx.packeteditor.data.IField;

import java.util.List;
import java.util.Map;

public class FieldMetadata {
    
    private String id;
    
    private String name;
    
    private IField.Type type;
    
    private Map<String, JsonElement> dictionary;

    private List<BitFlagMetadata> bits;

    public FieldMetadata(String id, String name, IField.Type type, Map<String, JsonElement> dictionary, List<BitFlagMetadata> bits) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.dictionary = dictionary;
        this.bits = bits;
    }

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

    public Map<String, JsonElement> getDictionary() {
        return dictionary;
    }
}
