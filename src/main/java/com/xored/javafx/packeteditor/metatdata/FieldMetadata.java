package com.xored.javafx.packeteditor.metatdata;

import com.google.gson.JsonElement;
import com.xored.javafx.packeteditor.data.FieldRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.*;

public class FieldMetadata {
    static Logger logger = LoggerFactory.getLogger(FieldMetadata.class);

    private String id;
    private String name;
    private FieldType type;
    private Boolean auto;
    private FieldRules fieldRules;
    private Map<String, JsonElement> dictionary;
    private List<BitFlagMetadata> bits;


    public enum FieldType {
        STRING,
        NUMBER,
        ENUM,
        BYTES, // used for payload. can be displayed as text if possible(printable ascii)
        BITMASK,
        METAFIELD, // TODO: support this for complex fields like a TCP_OPTIONS, IP_OPTIONS and replace it
        TCP_OPTIONS, // TODO: remove me
        IP_OPTIONS,  // TODO: remove me
        MAC_ADDRESS,
        IP_ADDRESS,
        UNKNOWN,
    }

    public FieldMetadata(
            String id, String name, FieldType type,
            Map<String, JsonElement> dictionary, List<BitFlagMetadata> bits, Boolean auto) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.dictionary = dictionary;
        this.bits = bits;
        this.auto = auto;
    }

    public FieldMetadata(
            String id, String name, FieldType type,
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

    public FieldType getType() {
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

    public static FieldType fieldTypeFromString(String type) {
        if (type == null) {
            return STRING;
        }
        try {
            return FieldMetadata.FieldType.valueOf(type);
        } catch (Exception e) {
            logger.error("Failed to parse enum value {}", e);
            return STRING;
        }
    }
}
