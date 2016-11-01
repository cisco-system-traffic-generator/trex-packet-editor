package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * structure of scapy.get_definitions
 * see https://github.com/cisco-system-traffic-generator/trex-doc/blob/master/trex_scapy_rpc_server.asciidoc
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
        public String type;
        public Boolean auto;
        public Integer min;
        public Integer max;
        public String regex;
        public JsonObject values_dict;
        public JsonArray bits;
    }

    public List<ScapyProtocol> protocols;
}
