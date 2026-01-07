package com.jackpf.locationhistory.client.ssl;

import java.security.cert.CertificateException;

import lombok.Getter;

public class UntrustedCertException extends CertificateException {
    @Getter
    private final String fingerprint;

    public UntrustedCertException(String fingerprint, Throwable cause) {
        super("Untrusted Certificate: " + fingerprint, cause);
        this.fingerprint = fingerprint;
    }

    public static boolean isCauseOf(Throwable t) {
        return getCauseFrom(t) != null;
    }

    public static UntrustedCertException getCauseFrom(Throwable t) {
        Throwable cause = t;

        while (cause != null) {
            if (cause instanceof UntrustedCertException) {
                return (UntrustedCertException) cause;
            }
            cause = cause.getCause();
        }
        return null;
    }
}
