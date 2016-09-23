package com.xored.javafx.packeteditor.scapy;

import java.util.ArrayList;
import java.util.List;

public class ReconstructProtocol {
    String id;
    Boolean delete;
    List<ReconstructField> fields;

    public ReconstructProtocol(String id) { this.id = id; }

    /** marks protocol and all sub-protocols for deletion */
    public static ReconstructProtocol deleteIt(String id) {
        ReconstructProtocol res = new ReconstructProtocol(id);
        res.delete = true;
        return res;
    }

    /** no changes. required to build paths */
    public static ReconstructProtocol pass(String id) {
        ReconstructProtocol res = new ReconstructProtocol(id);
        return res;
    }

    /** modifies fields*/
    public static ReconstructProtocol modify(String id, List<ReconstructField> fields) {
        ReconstructProtocol res = new ReconstructProtocol(id);
        res.fields = fields;
        return res;
    }
}
