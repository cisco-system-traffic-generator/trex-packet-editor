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
    
    public JsonElement vm_instructions;
    
    public List<InstructionExpressionData> vm_instructions_expressions = new ArrayList<>();
    
    public List<ProtocolData> data = new ArrayList<>();
    public String binary = ""; // binary packet data in base64 encoding

    public byte[] getPacketBytes() { return Base64.getDecoder().decode(binary); }
    public List<ProtocolData> getProtocols() { return data; }

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
        if (vm_instructions == null) {
            return Collections.<String, Object>emptyMap();
        }
        Gson gson = buildGson();
        return gson.fromJson(vm_instructions.toString(), new TypeToken<Map<String, Object>>(){}.getType());
    }
    
    private Gson buildGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(new TypeToken<Map <String, Object>>(){}.getType(),  new MapDeserializerDoubleAsInt());

        return gsonBuilder.create();
    }
}
