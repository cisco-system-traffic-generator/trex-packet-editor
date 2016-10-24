package com.xored.javafx.packeteditor.data.combined;

import com.xored.javafx.packeteditor.data.user.Document;
import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.FieldMetadata;
import com.xored.javafx.packeteditor.scapy.ProtocolData;
import com.xored.javafx.packeteditor.service.IMetadataService;
import javafx.scene.control.TitledPane;

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
        for (FieldMetadata field : protocolObj.getMeta().getFields()) {
            CombinedField cfield = new CombinedField();
            cfield.parent = protocolObj;
            cfield.meta = field;
            if (protocolObj.scapyProtocol != null) {
                cfield.scapyField = protocolObj.scapyProtocol.getFieldById(field.getId());
            }
            if (protocolObj.userProtocol != null) {
                cfield.userField = protocolObj.userProtocol.getField(field.getId());
            }
            protocolObj.fields.add(cfield);
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

    public boolean[] getProtocolsExpanded() {
        int count[] = {getProtocolStack().size(), 0};
        boolean expanded[] = new boolean[count[0]];
        getProtocolStack().stream().forEach(proto -> {
            UserProtocol userProtocol = proto.getUserProtocol();
            TitledPane pane = userProtocol.getTitledPane();
            if (count[1] < count[0]) {
                if (pane != null) {
                    expanded[count[1]++] = pane.isExpanded();
                } else {
                    expanded[count[1]++] = true;
                }
            }
        });
        return expanded;
    }

    public void setProtocolsExpanded(boolean expanded[]) {
        int count[] = new int[2];
        count[0] = Math.min(expanded.length, getProtocolStack().size());
        count[1] = 0;
        getProtocolStack().stream().forEach(proto -> {
            UserProtocol userProtocol = proto.getUserProtocol();
            TitledPane pane = userProtocol.getTitledPane();
            if (pane != null && count[1] < count[0]) {
                userProtocol.getTitledPane().setExpanded(expanded[count[1]++]);
            }
        });
    }

    public void setProtocolExpanded(boolean expanded) {
        getProtocolStack().stream().forEach(proto -> {
            UserProtocol userProtocol = proto.getUserProtocol();
            userProtocol.getTitledPane().setExpanded(expanded);
        });
    }
}
