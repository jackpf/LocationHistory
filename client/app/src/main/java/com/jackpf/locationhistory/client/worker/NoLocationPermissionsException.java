package com.jackpf.locationhistory.client.worker;

public class NoLocationPermissionsException extends Exception {
    public NoLocationPermissionsException() {
        super("No location permissions");
    }
}
