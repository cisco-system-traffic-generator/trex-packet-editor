package com.xored.javafx.packeteditor.scapy;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

/**
 * This is class is a result of build_pkt, reconstruct_pkt
 */
public class PacketData {
    
    public JsonObject field_engine;
    
    public List<InstructionExpressionData> vm_instructions_expressions = new ArrayList<>();
    
    public List<ProtocolData> data = new ArrayList<>();
    public String binary = ""; // binary packet data in base64 encoding

    public byte[] getPacketBytes() { return Base64.getDecoder().decode(binary); }
    public List<ProtocolData> getProtocols() { return data; }

    public String getFieldEngineError() {
        return field_engine != null && !(field_engine.get("error") instanceof JsonNull) ? field_engine.get("error").getAsString() : null;
    }
    public void setFieldEngineError(String error) {
        field_engine.add("error", new JsonPrimitive(error));
    }

    private class MapDeserializerDoubleAsInt implements JsonDeserializer<Map<String, Object>>{

        @Override  @SuppressWarnings("unchecked")
        public Map<String, Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return (Map<String, Object>) read(json);
        }

        public Object read(JsonElement in) {

            if(in.isJsonArray()){
                List<Object> list = new ArrayList<Object>();
                JsonArray arr = in.getAsJsonArray();
                for (JsonElement anArr : arr) {
                    list.add(read(anArr));
                }
                return list;
            }else if(in.isJsonObject()){
                Map<String, Object> map = new LinkedTreeMap<String, Object>();
                JsonObject obj = in.getAsJsonObject();
                Set<Map.Entry<String, JsonElement>> entitySet = obj.entrySet();
                for(Map.Entry<String, JsonElement> entry: entitySet){
                    map.put(entry.getKey(), read(entry.getValue()));
                }
                return map;
            }else if( in.isJsonPrimitive()){
                JsonPrimitive prim = in.getAsJsonPrimitive();
                if(prim.isBoolean()){
                    return prim.getAsBoolean();
                }else if(prim.isString()){
                    return prim.getAsString();
                }else if(prim.isNumber()){
                    return prim.getAsInt();
                }
            }
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPktVmInstructions() {
        if (field_engine == null) {
            return Collections.<String, Object>emptyMap();
        }
        JsonObject instructions = field_engine.get("instructions").getAsJsonObject();
        if (instructions.size() == 0) {
            return Collections.<String, Object>emptyMap();
        }
        Gson gson = buildGson();
        Map<String, Object> vm = gson.fromJson(instructions.toString(), new TypeToken<Map<String, Object>>() {}.getType());

        Map<String, Object> result = new HashMap<>();
        result.put("vm", vm);

        return result;
    }
    
    private Gson buildGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(new TypeToken<Map <String, Object>>(){}.getType(),  new MapDeserializerDoubleAsInt());

        return gsonBuilder.create();
    }
}
