package com.xored.javafx.packeteditor.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Observable;

public class BinaryData extends Observable implements IBinaryData {

    private Logger logger= LoggerFactory.getLogger(BinaryData.class);
    
    public enum OP {
        SET_BYTE, SET_BYTES, SELECTION, RELOAD
    }

    private byte[] bytes;
    private int selOffset;
    private int selLength;

    @Override
    public void setBytes(byte[] payload) {
        selOffset = 0;
        selLength = 0;
        bytes = payload;
        setChanged();
        notifyObservers(OP.RELOAD);
    }

    @Override
    public byte getByte(int idx) {
        return bytes[idx];
    }

    @Override
    public int getLength() {
        return bytes != null ? bytes.length: 0 ;
    }

    @Override
    public void setByte(int idx, byte value) {
        bytes[idx] = value;
        logger.info("Set bytes[{}] = {}", idx, (int) value);
        setChanged();
        notifyObservers(OP.SET_BYTE);
    }

    public byte[] getBytes(int offset, int length) {
        return Arrays.copyOfRange(bytes, offset, offset + length);
    }

    @Override
    public void setBytes(int offset, int length, byte[] bytes) {
        System.arraycopy(bytes, 0, this.bytes, offset, length);
        setChanged();
        notifyObservers(OP.SET_BYTES);
    }

    @Override
    public Observable getObservable() {
        return this;
    }

    @Override
    public void setSelected(int offset, int length) {
        selOffset = offset;
        selLength = length;

        setChanged();
        notifyObservers(OP.SELECTION);
    }

    public int getSelOffset() {
        return selOffset;
    }

    public int getSelLength() {
        return selLength;
    }
}
