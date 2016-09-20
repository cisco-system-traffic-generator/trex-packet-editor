package com.xored.javafx.packeteditor.scapy;

/** TODO: checked exception? */
public class ScapyException extends RuntimeException {
    ScapyException() { super("internal error"); }
    ScapyException(String message) { super(message); }
}
