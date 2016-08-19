package com.xored.javafx.packeteditor.data;

import java.util.Observable;

public interface IBinaryData {
    byte getByte(int idx);
    int getLength();
    void setByte(int idx, byte value);
    byte[] getBytes(int offset, int length);
    void setBytes(int offset, int length, byte[] bytes);
    Observable getObservable();
    void setSelected(int offset, int length);
    int getSelOffset();
    int getSelLength();
}
