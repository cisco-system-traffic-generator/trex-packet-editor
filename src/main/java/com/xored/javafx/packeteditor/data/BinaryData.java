package com.xored.javafx.packeteditor.data;

import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;

import java.util.Arrays;
import java.util.Observable;

public class BinaryData extends Observable implements IBinaryData {
    public enum OP {
        SET_BYTE, SET_BYTES, SELECTION
    }

    private final byte[] bytes;
    private int selOffset;
    private int selLength;

    public BinaryData() {
        ScapyServerClient scapy = new ScapyServerClient();
        scapy.open("tcp://localhost:4507");
        ScapyPkt ethernetPkt = scapy.getHttpPkt();
        bytes = ethernetPkt.getBinaryData();
    }

    @Override
    public byte getByte(int idx) {
        return bytes[idx];
    }

    @Override
    public int getLength() {
        return bytes.length;
    }

    @Override
    public void setByte(int idx, byte value) {
        bytes[idx] = value;
        System.out.println("[" + idx + "]=" + (int)value);
        setChanged();
        notifyObservers(OP.SET_BYTE);
    }

    public byte[] getBytes(int offset, int length) {
        return Arrays.copyOfRange(bytes, offset, offset + length);
    }

    @Override
    public void setBytes(int offset, int length, byte[] bytes) {
        for (int i = 0; i < length; i++) {
            this.bytes[offset + i] = bytes[i];
        }
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
