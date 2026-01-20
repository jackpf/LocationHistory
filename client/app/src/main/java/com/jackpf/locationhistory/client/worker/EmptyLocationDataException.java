package com.jackpf.locationhistory.client.worker;

public class EmptyLocationDataException extends Exception {
    public EmptyLocationDataException() {
        super("Empty location data");
    }
}
