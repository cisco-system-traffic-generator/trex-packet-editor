package com.xored.javafx.packeteditor.scapy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ReconstructFieldTest {
    Gson gson = new Gson();

    @Test
    public void should_delete_protocol() {
        String result = gson.toJson(Arrays.asList(
                ReconstructProtocol.pass("Ether"),
                ReconstructProtocol.deleteIt("IP")
        ));
        assertEquals(result, "[{\"id\":\"Ether\"},{\"id\":\"IP\",\"delete\":true}]");
    }

    @Test
    public void should_reset_field() {
        String result = gson.toJson(Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.resetValue("src")
                ))
        ));
        assertEquals(result, "[{\"id\":\"Ether\",\"fields\":[{\"id\":\"src\",\"delete\":true}]}]");
    }

    @Test
    public void should_set_hvalue() {
        String result = gson.toJson(Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.setHumanValue("type", "0x800")
                ))
        ));
        assertEquals(result, "[{\"id\":\"Ether\",\"fields\":[{\"id\":\"type\",\"hvalue\":\"0x800\"}]}]");
    }

    @Test
    public void should_set_value_int() {
        String result = gson.toJson(Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.setValue("type", 0x800)
                ))
        ));
        assertEquals(result, "[{\"id\":\"Ether\",\"fields\":[{\"id\":\"type\",\"value\":2048}]}]");
    }

    @Test
    public void should_set_value_string() {
        String result = gson.toJson(Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.setValue("src", "de:ad:be:ef:de:ad")
                ))
        ));
        assertEquals(result, "[{\"id\":\"Ether\",\"fields\":[{\"id\":\"src\",\"value\":\"de:ad:be:ef:de:ad\"}]}]");
    }

    @Test
    public void should_set_value_json_element() {
        String result = gson.toJson(Arrays.asList(
                ReconstructProtocol.modify("Ether", Arrays.asList(
                        ReconstructField.setValue("src", "de:ad:be:ef:de:ad")
                ))
        ));
        assertEquals(result, "[{\"id\":\"Ether\",\"fields\":[{\"id\":\"src\",\"value\":\"de:ad:be:ef:de:ad\"}]}]");
    }

    @Test
    public void should_be_converted_to_json_element() {
        JsonArray testparams = new JsonArray();
        testparams.add(new JsonPrimitive("test"));
        testparams.add(gson.toJsonTree(
                ReconstructProtocol.pass("Ether")

        ));
        String result = gson.toJson(testparams);
        assertEquals(result, "[\"test\",{\"id\":\"Ether\"}]");
    }

    @Test
    public void should_deserialize_just_for_fun() {
        String payload = "[{\"id\":\"Ether\",\"fields\":[{\"id\":\"src\",\"value\":\"de:ad:be:ef:de:ad\"}]}]";
        ReconstructProtocol[] pojo_res = gson.fromJson(payload, ReconstructProtocol[].class);
        String result = gson.toJson(pojo_res);
        assertEquals(result, payload); // not bad, we can do this
    }

    @Test
    public void should_not_break_on_unknown_fields() {
        String payload = "[{\"id\":\"Ether\",\"ignoreme\":[1,2,3]}]";
        ReconstructProtocol[] pojo_res = gson.fromJson(payload, ReconstructProtocol[].class);
        String result = gson.toJson(pojo_res);
        assertEquals(result, "[{\"id\":\"Ether\"}]");
    }
}