package com.xored.javafx.packeteditor.service;


import com.xored.javafx.packeteditor.metatdata.FEInstructionParameterMeta;
import com.xored.javafx.packeteditor.metatdata.FeParameterMeta;
import com.xored.javafx.packeteditor.metatdata.InstructionExpressionMeta;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.ProtocolData;

import java.util.List;
import java.util.Map;

public interface IMetadataService {
    Map<String, ProtocolMetadata> getProtocols();
    Map<String, FeParameterMeta> getFeParameters();

    Map<String, InstructionExpressionMeta> getFeInstructions();

    ProtocolMetadata getProtocolMetadata(ProtocolData protocol);
    ProtocolMetadata getProtocolMetadataById(String protocolId);

    List<String> getAllowedPayloadForProtocol(String protocolId);

    Map<String, FEInstructionParameterMeta> getFeInstructionParameters();
}
