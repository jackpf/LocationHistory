package com.jackpf.locationhistory.client.client.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
