package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static com.xored.javafx.packeteditor.scapy.FieldValue.ObjectType.BYTES;
import static com.xored.javafx.packeteditor.scapy.FieldValue.ObjectType.EXPRESSION;

/** Stores value for fields
 * for value field structure see ReconstructField value
 * */
public class FieldValue {
    private static Logger logger = LoggerFactory.getLogger(FieldValue.class);
    /** special value type for custom objects like binary payloads(base64)
     * and python expressions.
     * STRING, NUMBER are JSON primitives, they are specified directly in JSON
     * */
    public enum ObjectType {
        UNDEFINED, // empty field value
        RANDOM, // random field value
        MACHINE, // machine bytes for field
        EXPRESSION, // python expression
        BYTES, // bytes array
        OBJECT, // object as is
    }

    public static JsonElement create(JsonPrimitive primitiveVal) {
        return primitiveVal;
    }

    public static JsonObject create(ObjectType valueType) {
        JsonObject val = new JsonObject();
        val.add("vtype", new JsonPrimitive(valueType.toString()));
        return val;
    }

    public static JsonObject create(ObjectType valueType, String propName, String propValue) {
        JsonObject val = new JsonObject();
        val.add("vtype", new JsonPrimitive(valueType.toString()));
        val.add(propName, new JsonPrimitive(propValue));
        return val;
    }

    /** returns a object value type for value or null if this is a primitive */
    public static ObjectType getObjectValueType(JsonElement valueObj) {
        try {
            if (valueObj instanceof JsonObject) {
                return ObjectType.valueOf(valueObj.getAsJsonObject().get("vtype").getAsString());
            }
        } catch (Exception e) {
            logger.error("failed to get field value type", e);
        }
        return null;
    }

    public static boolean isPrimitive(JsonElement valueObj) {
        return (valueObj instanceof JsonPrimitive);
    }

    private static byte[] getBase64Bytes(JsonElement valueObj) {
        String bytes_base64 = valueObj.getAsJsonObject().get("base64").getAsString();
        return Base64.getDecoder().decode(bytes_base64);
    }

    public static byte[] getBytes(JsonElement valueObj) {
        if (BYTES.equals(getObjectValueType(valueObj))) {
            return getBase64Bytes(valueObj);
        } else {
            return null;
        }
    }

    public static String getExpression(JsonElement valueObj) {
        if (EXPRESSION.equals(getObjectValueType(valueObj))) {
            return valueObj.getAsJsonObject().get("expr").getAsString();
        } else {
            return null;
        }
    }
}
