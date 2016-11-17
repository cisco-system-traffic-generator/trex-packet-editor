package com.xored.javafx.packeteditor.service;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.FieldRules;
import com.xored.javafx.packeteditor.events.ScapyClientConnectedEvent;
import com.xored.javafx.packeteditor.metatdata.*;
import com.xored.javafx.packeteditor.scapy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/** provides protocol metadata based on hand-crafted protocol definition file or Scapy definitions */
public class MetadataService implements IMetadataService {
    private static Logger logger = LoggerFactory.getLogger(IMetadataService.class);

    @Inject
    ScapyServerClient scapy;

    Map<String, ProtocolMetadata> protocols = new HashMap<>();
    Map<String, List<String>> payload_classes_cache = new HashMap<>();
    Map<String, FeParameterMeta> feParametersMeta = new HashMap<>();
    Map<String, InstructionExpressionMeta> feInstructionMetas = new HashMap<>();

    public void initialize() {
        // TODO: delete me once protocols json moved to scapy server.
    }
    
    @Subscribe
    public void handleScapyConnectedEvent(ScapyClientConnectedEvent event) {
        loadDefinitions();
    }

    public Map<String, ProtocolMetadata> getProtocols() {
        return protocols;
    }

    @Override
    public Map<String, FeParameterMeta> getFeParameters() {
        return feParametersMeta;
    }

    @Override
    public Map<String, InstructionExpressionMeta> getFeInstructions() {
        return feInstructionMetas;
    }

    private void loadDefinitions() {
        try {
            ScapyDefinitions definitions = scapy.get_definitions();

            Map<String, FEInstructionParameterMeta> feInstructionParameterMetas = new LinkedTreeMap<>();
            
            if (definitions.feInstructionParameters != null) {
                definitions.feInstructionParameters.stream()
                        .filter(param -> param.id != null)
                        .forEach(param -> feInstructionParameterMetas.put(param.id, new FEInstructionParameterMeta(param.type, param.id, param.name, param.defaultValue, param.dict)));
            }

            if (definitions.feParameters != null) {
                feParametersMeta.putAll(definitions.feParameters.stream()
                        .map(scapyFEParameter -> new FeParameterMeta(scapyFEParameter.id, scapyFEParameter.name, scapyFEParameter.type, scapyFEParameter.defaultValue))
                        .collect(Collectors.toMap(FeParameterMeta::getId, meta -> meta)));
            }
            
            definitions.protocols.forEach(proto -> {
                // merge definitions with the hand-crafted file. json has priority over metadata from scapy
                protocols.put(proto.id, new ProtocolMetadata(
                        proto.id,
                        proto.name,
                        proto.fields.stream().map(this::buildFieldMetadata).collect(Collectors.toList()),
                        feInstructionParameterMetas,
                        proto.fieldEngineAwareFields
                ));
            });
            
            if (definitions.feInstructions != null) {
                definitions.feInstructions.stream().forEach( instructionData -> {
                    List<FEInstructionParameterMeta> parameterMetas = instructionData.parameters.stream()
                            .map(feInstructionParameterMetas::get).collect(Collectors.toList());
                    
                    InstructionExpressionMeta meta = new InstructionExpressionMeta(instructionData.id, parameterMetas);
                    feInstructionMetas.put(instructionData.id, meta);
                });
            }
        } catch (Exception e) {
            logger.error("failed to load protocol defs from scapy: {}", e);
        }
    }

    /** builds field metadata from scapy field definition */
    private FieldMetadata buildFieldMetadata(ScapyDefinitions.ScapyField field) {
        String fieldName = field.name;
        FieldMetadata.FieldType ftype = FieldMetadata.fieldTypeFromString(field.type);
        boolean isAuto = field.auto != null ? field.auto.booleanValue() : false;
        FieldRules rules = new FieldRules(field.min, field.max, field.regex);
        Map<String, JsonElement> values_dict = null;
        List<BitFlagMetadata> bitFlags = new ArrayList<>();

        if (field.values_dict instanceof JsonObject) {
            JsonObject field_dict = field.values_dict.getAsJsonObject();
            if (field_dict.size() > 0) {
                ftype = FieldMetadata.FieldType.ENUM;
                values_dict = field.values_dict.getAsJsonObject().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }

        if (field.bits instanceof JsonArray) {
            for (Object o : field.bits) {
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
        return new FieldMetadata(field.id, fieldName, ftype, values_dict, bitFlags, isAuto, rules);
    }

    /** normally should not be used, since we should have get_definitions */
    private ProtocolMetadata buildProtocolMetaFromScapy(String protocolId) {
        PacketData pkt = scapy.build_pkt(Arrays.asList(ReconstructProtocol.pass(protocolId)));
        if (pkt.getProtocols().isEmpty())
            return null;
        return buildMetadataFromScapyModel(pkt.getProtocols().get(0));
    }

    /** normally should not be used, since we should have get_definitions */
    private ProtocolMetadata buildMetadataFromScapyModel(ProtocolData protocol) {
        List<FieldMetadata> fields_metadata = protocol.fields.stream().map(
                field -> new FieldMetadata(field.id, field.id, FieldMetadata.FieldType.STRING, null, null, false)
        ).collect(Collectors.toList());
        ProtocolMetadata protocolMetadata = new ProtocolMetadata(protocol.id, protocol.name, fields_metadata, Collections.<String, FEInstructionParameterMeta>emptyMap(), new ArrayList<>());
        protocols.put(protocolMetadata.getId(), protocolMetadata);
        return protocolMetadata;
    }

    @Deprecated
    public ProtocolMetadata getProtocolMetadata(ProtocolData protocol) {
        return getProtocolMetadataById(protocol.id);
    }

    @Override
    public ProtocolMetadata getProtocolMetadataById(String protocolId) {
        ProtocolMetadata res = protocols.getOrDefault(protocolId, null);
        if (res == null) {
            logger.warn("Generating definition for {} protocol", protocolId);
            // TODO: parametrized get_definitions?
            res = buildProtocolMetaFromScapy(protocolId);
        }
        return res;
    }

    @Override
    public List<String> getAllowedPayloadForProtocol(String protocolId) {
        List<String> res = payload_classes_cache.getOrDefault(protocolId, null);
        if (res == null) {
            // Too slow, so getting lazily with cache
            res = scapy.get_payload_classes(protocolId);
            payload_classes_cache.put(protocolId, res);
        }
        return res;
    }
}

