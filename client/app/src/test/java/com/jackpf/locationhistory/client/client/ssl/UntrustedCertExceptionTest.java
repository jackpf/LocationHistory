package com.jackpf.locationhistory.client.client.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.security.cert.CertificateException;

public class UntrustedCertExceptionTest {

    private static final String TEST_FINGERPRINT = "AA:BB:CC:DD:EE:FF";

    @Test
    public void constructor_storesFingerprint() {
        CertificateException cause = new CertificateException("original cause");
        UntrustedCertException exception = new UntrustedCertException(TEST_FINGERPRINT, cause);

        assertEquals(TEST_FINGERPRINT, exception.getFingerprint());
    }

    @Test
    public void constructor_setsMessageWithFingerprint() {
        CertificateException cause = new CertificateException("original cause");
        UntrustedCertException exception = new UntrustedCertException(TEST_FINGERPRINT, cause);

        assertTrue(exception.getMessage().contains(TEST_FINGERPRINT));
        assertTrue(exception.getMessage().contains("Untrusted Certificate"));
    }

    @Test
    public void constructor_setsCause() {
        CertificateException cause = new CertificateException("original cause");
        UntrustedCertException exception = new UntrustedCertException(TEST_FINGERPRINT, cause);

        assertSame(cause, exception.getCause());
    }

    @Test
    public void isCauseOf_returnsTrueWhenDirectCause() {
        UntrustedCertException exception = new UntrustedCertException(TEST_FINGERPRINT, null);

        assertTrue(UntrustedCertException.isCauseOf(exception));
    }

    @Test
    public void isCauseOf_returnsTrueWhenNestedCause() {
        UntrustedCertException untrusted = new UntrustedCertException(TEST_FINGERPRINT, null);
        IOException wrapped = new IOException("wrapped", untrusted);
        RuntimeException outerWrapper = new RuntimeException("outer", wrapped);

        assertTrue(UntrustedCertException.isCauseOf(outerWrapper));
    }

    @Test
    public void isCauseOf_returnsFalseWhenNotInChain() {
        IOException exception = new IOException("not untrusted");

        assertFalse(UntrustedCertException.isCauseOf(exception));
    }

    @Test
    public void isCauseOf_returnsFalseForNull() {
        assertFalse(UntrustedCertException.isCauseOf(null));
    }

    @Test
    public void getCauseFrom_returnsExceptionWhenDirect() {
        UntrustedCertException exception = new UntrustedCertException(TEST_FINGERPRINT, null);

        UntrustedCertException result = UntrustedCertException.getCauseFrom(exception);

        assertSame(exception, result);
    }

    @Test
    public void getCauseFrom_returnsExceptionWhenNested() {
        UntrustedCertException untrusted = new UntrustedCertException(TEST_FINGERPRINT, null);
        IOException wrapped = new IOException("wrapped", untrusted);
        RuntimeException outerWrapper = new RuntimeException("outer", wrapped);

        UntrustedCertException result = UntrustedCertException.getCauseFrom(outerWrapper);

        assertSame(untrusted, result);
        assertEquals(TEST_FINGERPRINT, result.getFingerprint());
    }

    @Test
    public void getCauseFrom_returnsNullWhenNotInChain() {
        IOException exception = new IOException("not untrusted");

        UntrustedCertException result = UntrustedCertException.getCauseFrom(exception);

        assertNull(result);
    }

    @Test
    public void getCauseFrom_returnsNullForNull() {
        assertNull(UntrustedCertException.getCauseFrom(null));
    }

    @Test
    public void getCauseFrom_handlesDeepNesting() {
        UntrustedCertException untrusted = new UntrustedCertException(TEST_FINGERPRINT, null);
        Throwable current = untrusted;
        for (int i = 0; i < 10; i++) {
            current = new Exception("level " + i, current);
        }

        UntrustedCertException result = UntrustedCertException.getCauseFrom(current);

        assertNotNull(result);
        assertEquals(TEST_FINGERPRINT, result.getFingerprint());
    }
}
