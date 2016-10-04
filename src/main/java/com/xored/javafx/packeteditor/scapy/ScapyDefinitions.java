package com.xored.javafx.packeteditor.scapy;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * structure of scapy.get_definitions
 */
public class ScapyDefinitions {
    public class Protocol {
        public String id;
        public String name;
        public List<Field> fields;
    }

    public class Field {
        public String id;
        public String name;
        public String field_type;
        public JsonElement values_dict;
    }

    public List<Protocol> protocols;
}
