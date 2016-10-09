package com.xored.javafx.packeteditor.data.combined;

import com.xored.javafx.packeteditor.data.user.Document;
import com.xored.javafx.packeteditor.scapy.FieldData;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import com.xored.javafx.packeteditor.service.IMetadataService;

import java.util.ArrayList;
import java.util.List;

/**
 * used to show user and scapy models aside
 */
public class CombinedProtocolModel {
    public List<CombinedProtocol> getProtocolStack() {
        return protocolStack;
    }

    List<CombinedProtocol> protocolStack = new ArrayList<>();

    public static CombinedProtocolModel fromScapyData(IMetadataService metadataService, Document userModel, List<ProtocolData> scapyStack) {
        CombinedProtocolModel res = new CombinedProtocolModel();

        List<String> currentPath = new ArrayList<>();
        for (ProtocolData protocol : scapyStack) {
            currentPath = new ArrayList<>(currentPath);
            currentPath.add(protocol.id);

            CombinedProtocol protocolObj = new CombinedProtocol();
            protocolObj.meta = metadataService.getProtocolMetadata(protocol);
            protocolObj.path = currentPath;
            protocolObj.scapyProtocol = protocol;

            for (FieldData field : protocol.fields) {
                CombinedField cfield = new CombinedField();
                cfield.parent = protocolObj;
                cfield.meta = protocolObj.meta.getMetaForField(field.id);
                cfield.scapyField = field;
                protocolObj.fields.add(cfield);
            }

            res.protocolStack.add(protocolObj);
        }
        return res;
    }

}
