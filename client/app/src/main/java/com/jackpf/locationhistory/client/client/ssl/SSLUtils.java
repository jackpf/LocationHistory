package com.jackpf.locationhistory.client.client.ssl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class SSLUtils {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(":");
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

    public static String getSha256Fingerprint(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] publicKey = md.digest(cert.getEncoded());
            return bytesToHex(publicKey);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new RuntimeException("Failed to generate SHA-256 fingerprint", e);
        }
    }
}
