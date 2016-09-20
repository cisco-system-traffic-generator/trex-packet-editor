package com.xored.javafx.packeteditor.data;

import java.util.ArrayList;
import java.util.List;


public class JPacket extends ArrayList<JPacket.Proto> {

    public JPacket (List<JPacket.Proto> list)       {super(list);}

    @Override
    public String toString () {
        return listToString(this);
    }

    static String listToString (List list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Object e : list) {
            if (sb.length() > 1)
                sb.append(',');
            sb.append(e.toString());
        }
        sb.append(']');
        return sb.toString();
    }


    public static class Proto {
        final String id;
        public List<Field> fields;

        public Proto (String i)                     {id = i; fields = new ArrayList<>();}
        public Proto (String i, List<Field> list)   {id = i; fields = list;}

        @Override
        public String toString () {
            return "{\"id\":\""+id+"\",\"fields\":"+JPacket.listToString(fields)+"}";
        }
    }


    public static class Field {
        final String id;
        public Object value;

        public Field (String i, Object  v)          {id = i; value = v;}

        @Override
        public String toString () {
            String quote = value instanceof String ? "\"" : "";
            return "{\"id\":\""+id+"\",\"value\":"+quote+value+quote+"}";
        }
    }
}
