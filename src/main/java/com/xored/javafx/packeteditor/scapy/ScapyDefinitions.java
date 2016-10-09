package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * structure of scapy.get_definitions
 */
public class ScapyDefinitions {
    public class ScapyProtocol {
        public String id;
        public String name;
        public List<ScapyField> fields;
    }

    public class ScapyField {
        public String id;
        public String name;
        public String field_type;
        public JsonElement values_dict;
    }

    public List<ScapyProtocol> protocols;
}
