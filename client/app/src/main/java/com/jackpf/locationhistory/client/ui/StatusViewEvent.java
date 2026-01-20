package com.jackpf.locationhistory.client.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public abstract class StatusViewEvent {
    public enum Type {UPDATE_CONNECTION_STATUS, UPDATE_DEVICE_STATE, SHOW_SSL_PROMPT}

    public abstract Type getType();

    @Getter
    @RequiredArgsConstructor
    public static class UpdateConnectionStatus extends StatusViewEvent {
        @Override
        public Type getType() {
            return Type.UPDATE_CONNECTION_STATUS;
        }

        private final int statusResId;
    }

    @Getter
    @RequiredArgsConstructor
    public static class UpdateDeviceState extends StatusViewEvent {
        @Override
        public Type getType() {
            return Type.UPDATE_DEVICE_STATE;
        }

        private final String deviceState;
        private final String lastPing;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ShowSslPrompt extends StatusViewEvent {
        @Override
        public Type getType() {
            return Type.SHOW_SSL_PROMPT;
        }

        private final String fingerprint;
    }
}
