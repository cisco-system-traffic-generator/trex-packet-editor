package com.xored.javafx.packeteditor.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.data.IField;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.*;

import java.util.*;
import java.util.stream.Collectors;

/** provides protocol metadata based on hand-crafted protocol definition file or Scapy definitions */
public class MetadataService implements IMetadataService {
    final int max_enum_values_to_display = 100; // max sane number of choice enumeration.

    @Inject
    LocalFileMetadataService localFileMetadataService;

    @Inject
    ScapyServerClient scapy;

    Map<String, ProtocolMetadata> protocols = new HashMap<>();

    public void initialize() {
        localFileMetadataService.initialize();
        protocols.putAll(localFileMetadataService.getProtocols());
    }

    public Map<String, ProtocolMetadata> getProtocols() {
        return protocols;
    }

    /** build dummy scapy object and retrieve field metadata */
    private ProtocolMetadata buildProtocolMetaFromScapy(String protocolId) {
        PacketData pkt = scapy.build_pkt(Arrays.asList(ReconstructProtocol.pass(protocolId)));
        if (pkt.getProtocols().isEmpty())
            return null;
        return buildMetadataFromScapyModel(pkt.getProtocols().get(0));
    }

    public ProtocolMetadata buildMetadataFromScapyModel(ProtocolData protocol) {
        List<FieldMetadata> fields_metadata = protocol.fields.stream().map(this::buildFieldMetaFromScapy).collect(Collectors.toList());
        ProtocolMetadata protocolMetadata = new ProtocolMetadata(protocol.id, protocol.name, fields_metadata, new ArrayList<>());
        protocols.put(protocolMetadata.getId(), protocolMetadata);
        return protocolMetadata;
    }

    public ProtocolMetadata getProtocolMetadata(ProtocolData protocol) {
        return getProtocolMetadataById(protocol.id);
        // TODO: alternatively, we can get meta w/o access to scapy
        //ProtocolMetadata res = localFileMetadataService.getProtocolMetadataById(protocol.id);
        //return res != null ? res : buildMetadataFromScapyModel(protocol);
    }

    public FieldMetadata buildFieldMetaFromScapy(FieldData field) {
        JsonObject dict = field.values_dict;
        if (dict != null && dict.size() > 0 && dict.size() < max_enum_values_to_display) {
            Map<String, JsonElement> dict_map = dict.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return new FieldMetadata(field.id, field.id, IField.Type.ENUM, dict_map, null);

        } else {
            return new FieldMetadata(field.id, field.id, IField.Type.STRING, null, null);
        }
    }

    @Override
    public ProtocolMetadata getProtocolMetadataById(String protocolId) {
        //ProtocolMetadata res = localFileMetadataService.getProtocolMetadataById(protocolId);
        //return res != null ? res : buildProtocolMetaFromScapy(protocolId);
        if (protocols.containsKey(protocolId)) {
            return protocols.get(protocolId);
        } else {
            return buildProtocolMetaFromScapy(protocolId);
        }
    }
}
