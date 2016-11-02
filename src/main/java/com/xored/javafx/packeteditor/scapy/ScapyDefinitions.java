package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Map;

/**
 * structure of scapy.get_definitions
 */
public class ScapyDefinitions {
    public class ScapyProtocol {
        public String id;
        public String name;
        public List<ScapyField> fields;
        public List<String> fieldEngineAwareFields;
    }

    public class ScapyField {
        public String id;
        public String name;
        public String field_type;
        public JsonElement values_dict;
    }

    public class ScapyFEInstructionParameter {
        public String type;
        public String id;
        public String name;
        public String defaultValue;
        public Map<String, String> dict;
    }

    public List<ScapyProtocol> protocols;

    public List<ScapyFEInstructionParameter> feInstructionParameters;
}
