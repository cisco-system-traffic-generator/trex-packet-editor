package com.xored.javafx.packeteditor.data.combined;

import com.xored.javafx.packeteditor.data.user.Document;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import com.xored.javafx.packeteditor.service.IMetadataService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * used to show user and scapy models aside
 */
public class CombinedProtocolModel {
    List<CombinedProtocol> protocolStack = new ArrayList<>();

    public List<CombinedProtocol> getProtocolStack() {
        return protocolStack;
    }

    public String getLastProtocolId() {
        return protocolStack.get(protocolStack.size() - 1).getId();
    }

    public static CombinedProtocolModel fromScapyData(IMetadataService metadataService, Document userModel, List<ProtocolData> scapyStack) {
        CombinedProtocolModel res = new CombinedProtocolModel();

        List<String> currentPath = new ArrayList<>();
        for (ProtocolData protocol : scapyStack) {
            currentPath = new ArrayList<>(currentPath);
            currentPath.add(protocol.getId());

            CombinedProtocol protocolObj = new CombinedProtocol();
            protocolObj.meta = metadataService.getProtocolMetadataById(protocol.getId());
            protocolObj.path = currentPath;
            protocolObj.scapyProtocol = protocol;

            createFields(protocolObj);

            res.protocolStack.add(protocolObj);
        }
        return res;
    }

    public static CombinedProtocolModel fromUserModel(IMetadataService metadataService, Document userModel, List<ProtocolData> scapyStack) {
        CombinedProtocolModel res = new CombinedProtocolModel();

        List<String> currentPath = new ArrayList<>();
        for (UserProtocol protocol : userModel.getProtocolStack()) {
            currentPath = new ArrayList<>(currentPath);
            currentPath.add(protocol.getId());

            CombinedProtocol protocolObj = new CombinedProtocol();
            protocolObj.meta = metadataService.getProtocolMetadataById(protocol.getId());
            protocolObj.path = currentPath;
            protocolObj.userProtocol = protocol;
            protocolObj.scapyProtocol = getByPath(currentPath, scapyStack);

            createFields(protocolObj);

            res.protocolStack.add(protocolObj);
        }
        return res;
    }

    private static void createFields(CombinedProtocol protocolObj) {
        for (FieldMetadata fieldMeta : protocolObj.getMeta().getFields()) {
            CombinedField field = new CombinedField();
            
            field.parent = protocolObj;
            field.meta = fieldMeta;
            if (protocolObj.scapyProtocol != null) {
                field.scapyField = protocolObj.scapyProtocol.getFieldById(fieldMeta.getId());
            }
            if (protocolObj.userProtocol != null) {
                field.userField = protocolObj.userProtocol.getField(fieldMeta.getId());
            }
            protocolObj.fields.add(field);
        }
    }

    static public ProtocolData getByPath(List<String> path, List<ProtocolData> stack) {
        ProtocolData res = null;
        Iterator<String> pathIt = path.iterator();
        Iterator<ProtocolData> stackIt = stack.iterator();
        while (pathIt.hasNext()) {
            if (!stackIt.hasNext()) {
                // path is longer
                return null;
            }
            res = stackIt.next();
            if (!pathIt.next().equals(res.getId())) {
                // id mismatch
                return null;
            }
        }
        return res;
    }
}
