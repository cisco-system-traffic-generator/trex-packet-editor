package com.xored.javafx.packeteditor.service;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xored.javafx.packeteditor.controllers.MenuController;
import com.xored.javafx.packeteditor.data.IField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.BitFlagMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.xored.javafx.packeteditor.data.IField.Type.*;

public class LocalFileMetadataService implements IMetadataService {

    private Logger logger= LoggerFactory.getLogger(MenuController.class); 
    
    private Gson gson = new Gson();
    
    private Map<String, ProtocolMetadata> protocols = new HashMap<>();
    
    
    
    
    @Override
    public Map<String, ProtocolMetadata> getProtocols() {
        return protocols;
    }
    
    public ProtocolMetadata getProtocolMetadataById(String protocolId) {
        return protocols.get(protocolId);
    }

    @Override
    public void initialize() {
        try {
            loadMeta();
        } catch (IOException e) {
            logger.error("Unable to init LocalFileMetadataService due to: {}", e.getLocalizedMessage());
        }
    }

    public void loadMeta() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File metadataFile = new File(classLoader.getResource("protocols/protocols.json").getFile());
        BufferedReader br = new BufferedReader(new FileReader(metadataFile));
        List<JsonObject> metadata = Arrays.asList(gson.fromJson(br, JsonObject[].class));
        metadata.stream().forEach((entry) -> {
            List<FieldMetadata> fieldsMeta = new ArrayList<>();
            Iterator<JsonElement> fieldsIt = entry.get("fields").getAsJsonArray().iterator();
            while (fieldsIt.hasNext()) {
                JsonObject field = (JsonObject) fieldsIt.next();

                String fieldId = field.get("id").getAsString();
                String name = field.get("name").getAsString();
                String typeName = field.get("type").getAsString();
                IField.Type type = getTypeByName(typeName);
                Map<String, JsonElement> dict = Collections.EMPTY_MAP;
                List<BitFlagMetadata> bitFlags = new ArrayList<>();

                if (ENUM.equals(type)) {
                    dict = field.getAsJsonObject("dict").entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }

                if(BITMASK.equals(type)) {
                    Iterator bitsIT = field.getAsJsonArray("bits").iterator();
                    
                    while(bitsIT.hasNext()) {
                        JsonObject bitFlag = (JsonObject) bitsIT.next();
                        
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

                fieldsMeta.add(new FieldMetadata(fieldId, name, type, dict, bitFlags));
            }

            List<String> payload = new ArrayList<>();

            Iterator<JsonElement> payloadIt = entry.get("payload").getAsJsonArray().iterator();
            while (payloadIt.hasNext()) {
                payload.add(payloadIt.next().getAsString());
            }

            ProtocolMetadata protocol = new ProtocolMetadata(entry.get("id").getAsString(), entry.get("name").getAsString(), fieldsMeta, payload);
            protocols.put(entry.get("id").getAsString(), protocol);
        });
    }
    
    private IField.Type getTypeByName(String id) {
        switch (id) {
            case "NUMBER":
                return NUMBER;
            case "IPv4Address":
                return IPV4ADDRESS;
            case "MACADDRESS":
                return MAC_ADDRESS;
            case "enum":
                return ENUM;
            case "TCPOptions":
                return TCP_OPTIONS;
            case "bitmask":
                return BITMASK;
            case "raw":
                return RAW;
            default:
                return IField.Type.STRING;
        }
    }
}
