package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Base64;

import static com.xored.javafx.packeteditor.scapy.FieldValue.ObjectType.RANDOM;
import static com.xored.javafx.packeteditor.scapy.FieldValue.ObjectType.UNDEFINED;

/** This class defines the change, which needs to be applied to protocol field
 * all fields except 'id' are optional.
 * */
public class ReconstructField {
    public String id; // required
    public JsonElement value;


    ReconstructField(String id, JsonElement value) {
        this.id = id;
        this.value = value;
    }

    /** sets random field value */
    public static ReconstructField randomizeValue(String fieldId) {
        return new ReconstructField(fieldId, FieldValue.create(RANDOM));
    }

    /** resets field value */
    public static ReconstructField resetValue(String fieldId) {
        return new ReconstructField(fieldId, FieldValue.create(UNDEFINED));
    }

    /** set human-value(hvalue) and let Scapy_server to guess the value type and convert from string */
    public static ReconstructField setHumanValue(String fieldId, String hvalue) {
        return setValue(fieldId, hvalue); // no special object. scapy-server should guess value type
    }

    /** set field value */
    public static ReconstructField setRawValue(String fieldId, JsonElement value) {
        return new ReconstructField(fieldId, value);
    }

    /** set field value */
    public static ReconstructField setValue(String fieldId, String value) {
        return new ReconstructField(fieldId, new JsonPrimitive(value));
    }

    /** set field value */
    public static ReconstructField setValue(String fieldId, Number value) {
        return new ReconstructField(fieldId, new JsonPrimitive(value));
    }

    /** set field value */
    public static ReconstructField setValue(String fieldId, byte[] bytes) {
        String bytes_base64 = Base64.getEncoder().encodeToString(bytes);
        return new ReconstructField(fieldId, FieldValue.create(FieldValue.ObjectType.BYTES, "base64", bytes_base64));
    }

    /** set raw eval python expression as a value */
    public static ReconstructField setExpressionValue(String fieldId, String expr) {
        return new ReconstructField(fieldId, FieldValue.create(FieldValue.ObjectType.EXPRESSION, "expr", expr));
    }

    public boolean isDeleted() {
        return UNDEFINED.equals(FieldValue.getObjectValueType(value));
    }

    public boolean isRandom() {
        return RANDOM.equals(FieldValue.getObjectValueType(value));
    }
}
