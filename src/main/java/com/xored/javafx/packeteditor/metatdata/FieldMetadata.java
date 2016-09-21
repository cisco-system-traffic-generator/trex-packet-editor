package com.xored.javafx.packeteditor.metatdata;

import com.xored.javafx.packeteditor.data.IField;

import java.util.Map;

public class FieldMetadata {
    
    private String id;
    
    private String name;
    
    private IField.Type type;
    
    private Map<String, String> dictionary;

    public FieldMetadata(String id, String name, IField.Type type, Map<String, String> dictionary) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.dictionary = dictionary;
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

    public Map<String, String> getDictionary() {
        return dictionary;
    }
}
