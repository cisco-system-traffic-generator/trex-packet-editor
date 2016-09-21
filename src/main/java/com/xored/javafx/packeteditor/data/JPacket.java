package com.xored.javafx.packeteditor.data;

import java.util.ArrayList;
import java.util.List;


public class JPacket extends ArrayList<JPacket.Proto> {

    public JPacket (List<JPacket.Proto> list) {
        super(list);
    }

    
    public static class Proto {
        public final String id;
        public int offset;
        public List<Field> fields;

        public Proto (String i)                     {id = i; fields = new ArrayList<>();}
        public Proto (String i, List<Field> list)   {id = i; fields = list;}
        
        public Field getField (String id) {
            for (Field f : fields)
                if (f.id.equals(id))
                    return f;
            return null;
        }
    }


    public static class Field {
        public final String id;
        public int offset,
                   length;
        public Object value;

        public Field (String i, Object  v)          {id = i; value = v;}
    }
}
