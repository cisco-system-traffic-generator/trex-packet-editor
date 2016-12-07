package com.xored.javafx.packeteditor.data.combined;

import com.xored.javafx.packeteditor.data.user.UserProtocol;
import com.xored.javafx.packeteditor.metatdata.ProtocolMetadata;
import com.xored.javafx.packeteditor.scapy.ProtocolData;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Returns protocol index(for duplicate entries) in packet structure.
     * For ex: Ether()\IP()\UDP()\IP()\UDP() -> IP will have two indexes IP:0 and IP:1
     * @return
     */
    public int getIdx() {
        return Math.max(0, Collections.frequency(path, getId()) - 1);
    }

    /**
     * Returns protocol identificator in the packet structure
     * @return
     */
    public String getCrumbId() {
        int idx = getIdx();
        String idxStr = idx > 0 ? ":" + idx : "";
        return "  "+ getId() + idxStr + "  ";
    }

    public String toString() {
        return getCrumbId();
    }
}
