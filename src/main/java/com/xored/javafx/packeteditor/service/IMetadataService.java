package com.xored.javafx.packeteditor.service;


import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.ProtocolData;

import java.util.List;
import java.util.Map;

public interface IMetadataService {
    Map<String, ProtocolMetadata> getProtocols();

    ProtocolMetadata getProtocolMetadata(ProtocolData protocol);
    ProtocolMetadata getProtocolMetadataById(String protocolId);

    List<String> getAllowedPayloadForProtocol(String protocolId);

    void initialize();
}
