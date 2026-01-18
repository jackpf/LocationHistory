package com.jackpf.locationhistory.client.ui;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public abstract class SettingsViewEvent {
    public enum Type { TOAST, SSL_PROMPT, SET_UNIFIED_PUSH_CHECKED, SHOW_DISTRIBUTOR_PICKER }

    public abstract Type getType();

    @Getter
    public static class Toast extends SettingsViewEvent {
        @Override public Type getType() { return Type.TOAST; }
        private final int messageResId;
        private final Object[] formatArgs;

        public Toast(int messageResId, Object... formatArgs) {
            this.messageResId = messageResId;
            this.formatArgs = formatArgs;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class SslPrompt extends SettingsViewEvent {
        @Override public Type getType() { return Type.SSL_PROMPT; }
        private final String fingerprint;
    }

    @Getter
    @RequiredArgsConstructor
    public static class SetUnifiedPushChecked extends SettingsViewEvent {
        @Override public Type getType() { return Type.SET_UNIFIED_PUSH_CHECKED; }
        private final boolean checked;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ShowDistributorPicker extends SettingsViewEvent {
        @Override public Type getType() { return Type.SHOW_DISTRIBUTOR_PICKER; }
        private final List<String> distributors;
    }
}
