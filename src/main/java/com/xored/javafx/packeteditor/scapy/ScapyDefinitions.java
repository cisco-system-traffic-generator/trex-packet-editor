package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * structure of scapy.get_definitions
 * see https://github.com/cisco-system-traffic-generator/trex-doc/blob/master/trex_scapy_rpc_server.asciidoc
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
        public String type;
        public Boolean auto;
        public Integer min;
        public Integer max;
        public String regex;
        public JsonObject values_dict;
        public JsonArray bits;
    }

    public class ScapyFEParameter {
        public String type;
        public String id;
        public String name;
        public String defaultValue;
        public Map<String, String> dict;
        public Boolean required;
        public Boolean editable;
    }

    public class ScapyFEInstruction {
        public String id;
        public String help;
        public List<String> parameters;
    }

    public List<ScapyProtocol> protocols;

    public List<ScapyFEParameter> feInstructionParameters;
    public List<ScapyFEInstruction> feInstructions;
    public List<ScapyFEParameter> feParameters;
}
