package com.jackpf.locationhistory.client.client.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class SSLUtilsTest {

    @Test
    public void bytesToHex_emptyArray_returnsEmptyString() {
        String result = SSLUtils.bytesToHex(new byte[0]);
        assertEquals("", result);
    }

    @Test
    public void bytesToHex_singleByte_returnsHex() {
        String result = SSLUtils.bytesToHex(new byte[]{(byte) 0xAB});
        assertEquals("AB", result);
    }

    @Test
    public void bytesToHex_multipleBytes_returnsColonSeparatedHex() {
        String result = SSLUtils.bytesToHex(new byte[]{(byte) 0xAB, (byte) 0xCD, (byte) 0xEF});
        assertEquals("AB:CD:EF", result);
    }

    @Test
    public void bytesToHex_zeroByte_paddsWithZero() {
        String result = SSLUtils.bytesToHex(new byte[]{0x00});
        assertEquals("00", result);
    }

    @Test
    public void bytesToHex_lowValue_paddsWithZero() {
        String result = SSLUtils.bytesToHex(new byte[]{0x0A});
        assertEquals("0A", result);
    }

    @Test
    public void bytesToHex_mixedValues_formatsCorrectly() {
        String result = SSLUtils.bytesToHex(new byte[]{0x00, 0x0F, (byte) 0xFF, 0x10});
        assertEquals("00:0F:FF:10", result);
    }

    @Test
    public void bytesToHex_allZeros_formatsCorrectly() {
        String result = SSLUtils.bytesToHex(new byte[]{0x00, 0x00, 0x00});
        assertEquals("00:00:00", result);
    }

    @Test
    public void bytesToHex_allOnes_formatsCorrectly() {
        String result = SSLUtils.bytesToHex(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        assertEquals("FF:FF:FF", result);
    }

    @Test
    public void bytesToHex_sha256Length_formatsCorrectly() {
        // SHA-256 produces 32 bytes
        byte[] sha256Bytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            sha256Bytes[i] = (byte) i;
        }

        String result = SSLUtils.bytesToHex(sha256Bytes);

        // Should have 32 hex pairs with 31 colons
        assertEquals(32 * 2 + 31, result.length());
        assertTrue(result.startsWith("00:01:02:03"));
    }

    @Test
    public void getSha256Fingerprint_returnsSha256OfEncodedCert() throws CertificateEncodingException {
        X509Certificate cert = mock(X509Certificate.class);
        // "hello" as bytes
        when(cert.getEncoded()).thenReturn(new byte[]{0x68, 0x65, 0x6c, 0x6c, 0x6f});

        String fingerprint = SSLUtils.getSha256Fingerprint(cert);

        // SHA-256 of "hello" is 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        assertNotNull(fingerprint);
        assertEquals(
                "2C:F2:4D:BA:5F:B0:A3:0E:26:E8:3B:2A:C5:B9:E2:9E:1B:16:1E:5C:1F:A7:42:5E:73:04:33:62:93:8B:98:24",
                fingerprint
        );
    }

    @Test
    public void getSha256Fingerprint_emptyEncoding_returnsHashOfEmpty() throws CertificateEncodingException {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getEncoded()).thenReturn(new byte[0]);

        String fingerprint = SSLUtils.getSha256Fingerprint(cert);

        // SHA-256 of empty byte array is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals(
                "E3:B0:C4:42:98:FC:1C:14:9A:FB:F4:C8:99:6F:B9:24:27:AE:41:E4:64:9B:93:4C:A4:95:99:1B:78:52:B8:55",
                fingerprint
        );
    }

    @Test
    public void getSha256Fingerprint_returnsCorrectLength() throws CertificateEncodingException {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getEncoded()).thenReturn(new byte[]{0x01, 0x02, 0x03});

        String fingerprint = SSLUtils.getSha256Fingerprint(cert);

        // SHA-256 is 32 bytes = 64 hex chars + 31 colons = 95 chars
        assertEquals(95, fingerprint.length());
    }

    @Test(expected = RuntimeException.class)
    public void getSha256Fingerprint_encodingException_throwsRuntimeException() throws CertificateEncodingException {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getEncoded()).thenThrow(new CertificateEncodingException("encoding failed"));

        SSLUtils.getSha256Fingerprint(cert);
    }
}
