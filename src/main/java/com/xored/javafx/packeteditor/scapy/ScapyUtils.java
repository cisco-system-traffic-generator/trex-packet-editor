package com.xored.javafx.packeteditor.scapy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScapyUtils {

    /** generates payload for reconstruct_pkt */
    public static List<ReconstructProtocol> createReconstructPktPayload(List<String> fieldPath, ReconstructField fieldEdit) {
        List<ReconstructProtocol> res = fieldPath.stream().map(
                protocolId->ReconstructProtocol.pass(protocolId)
        ).collect(Collectors.toList());

        if (!fieldPath.isEmpty()) {
            res.get(res.size() - 1).fields = Arrays.asList(fieldEdit);
        }
        return res;
    }

    public static boolean isAsciiChar(byte val) { return (int)val <= 127; }
    /** returns true if character can be displayed. https://en.wikipedia.org/wiki/ASCII#Printable_characters */
    public static boolean isPrintableChar(byte val) { return (int)val >= 0x20 && (int) val <= 0x7E; }

}

