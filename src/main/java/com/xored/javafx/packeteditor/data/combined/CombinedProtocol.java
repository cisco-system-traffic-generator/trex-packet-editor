package com.xored.javafx.packeteditor.data.combined;

import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.ProtocolData;

import java.util.ArrayList;
import java.util.List;

public class CombinedProtocol {
    ProtocolMetadata meta;
    List<String> path;
    List<CombinedField> fields = new ArrayList<>();
    UserProtocol userProtocol;
    ProtocolData scapyProtocol;

    public List<CombinedField> getFields() { return fields; }
    public UserProtocol getUserProtocol() { return userProtocol; }
    public ProtocolData getScapyProtocol() { return scapyProtocol; }

    public String getId() { return getMeta().getId(); }
    public ProtocolMetadata getMeta() { return meta; }

    public List<String> getPath() { return path; }
}
