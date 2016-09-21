package com.xored.javafx.packeteditor.service;


import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;

import java.util.Map;

public interface IMetadataService {
    public Map<String, ProtocolMetadata> getProtocols();
    public void initialize();
}
