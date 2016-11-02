package com.xored.javafx.packeteditor.service;


import com.google.gson.*;
import com.xored.javafx.packeteditor.data.FieldRules;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.BITMASK;
import static com.xored.javafx.packeteditor.metatdata.FieldMetadata.FieldType.ENUM;

class LocalFileMetadataService {

    private Logger logger = LoggerFactory.getLogger(LocalFileMetadataService.class);
    
    private Gson gson = new Gson();
    
    private Map<String, ProtocolMetadata> protocols = new HashMap<>();
    
    public Map<String, ProtocolMetadata> getProtocols() {
        return protocols;
    }
    
    public ProtocolMetadata getProtocolMetadataById(String protocolId) {
        return protocols.get(protocolId);
    }

    public void initialize() {
        try {
            loadMeta();
        } catch (IOException e) {
            logger.error("Unable to init LocalFileMetadataService due to: {}", e.getLocalizedMessage());
        }
    }

    public void loadMeta() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/protocols/protocols.json")));
        List<JsonObject> metadata = Arrays.asList(gson.fromJson(reader, JsonObject[].class));
        metadata.stream().forEach((entry) -> {
            List<FieldMetadata> fieldsMeta = new ArrayList<>();
            for (JsonElement jsonElement : entry.get("fields").getAsJsonArray()) {
                JsonObject field = (JsonObject) jsonElement;

                // TODO: use GSON parsing to internal class with JsonArray/JsonObject
                String fieldId = field.get("id").getAsString();
                String name = field.get("name").getAsString();
                Boolean isAuto = (field.get("auto") instanceof  JsonPrimitive) ? field.get("auto").getAsBoolean(): null;
                String typeName = getAttrStringValue(field, "type");
                Integer min = getAttrIntValue(field, "min");
                Integer max = getAttrIntValue(field, "max");
                String regex = getAttrStringValue(field, "regex");
                FieldRules fieldRules = new FieldRules(min, max, regex);
                
                FieldMetadata.FieldType type = FieldMetadata.fieldTypeFromString(typeName);
                Map<String, JsonElement> dict = null;
                List<BitFlagMetadata> bitFlags = new ArrayList<>();

                if (ENUM.equals(type)) {
                    dict = field.getAsJsonObject("dict").entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }

                if (BITMASK.equals(type)) {
                    for (Object o : field.getAsJsonArray("bits")) {
                        JsonObject bitFlag = (JsonObject) o;

                        JsonArray bitFlagValues = bitFlag.getAsJsonArray("values");
                        Iterator bitFlagValuesIT = bitFlagValues.iterator();

                        Map<String, JsonElement> vals = new HashMap<>();

                        while (bitFlagValuesIT.hasNext()) {
                            JsonObject bitFlagValue = (JsonObject) bitFlagValuesIT.next();
                            vals.put(bitFlagValue.get("name").getAsString(), bitFlagValue.get("value"));
                        }
                        String bitFlagName = bitFlag.get("name").getAsString();
                        Integer mask = bitFlag.get("mask").getAsInt();
                        BitFlagMetadata bitFlagMeta = new BitFlagMetadata(bitFlagName, mask, vals);
                        bitFlags.add(bitFlagMeta);
                    }
                }

                fieldsMeta.add(new FieldMetadata(fieldId, name, type, dict, bitFlags, isAuto, fieldRules));
            }

            /* TODO: restore or remove hand-crafted payload
            List<String> payload = new ArrayList<>();

            for (JsonElement jsonElement : entry.get("payload").getAsJsonArray()) {
                payload.add(jsonElement.getAsString());
            }
            */

            ProtocolMetadata protocol = new ProtocolMetadata(entry.get("id").getAsString(), entry.get("name").getAsString(), fieldsMeta, new ArrayList<>(), new ArrayList<>());
            protocols.put(entry.get("id").getAsString(), protocol);
        });
    }

    private String getAttrStringValue(JsonObject field, String attrName) {
        return (field.get(attrName) instanceof JsonPrimitive) ? field.get(attrName).getAsString() : null;
    }

    private Integer getAttrIntValue(JsonObject field, String attrName) {
        return (field.get(attrName) instanceof JsonPrimitive) ? field.get(attrName).getAsInt() : null;
    }
}
