package com.jackpf.locationhistory.client.ssl;

import android.content.SharedPreferences;

import com.jackpf.locationhistory.client.util.Logger;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class DynamicTrustManager implements X509TrustManager, AutoCloseable {
    private final TrustedCertStorage storage;
    private final X509TrustManager defaultTrustManager;
    private final Set<String> trustedFingerprints = new HashSet<>();
    private final Logger log = new Logger(this);

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (sharedPreferences, key) -> reloadTrustedFingerPrints();

    public DynamicTrustManager(TrustedCertStorage storage) throws NoSuchAlgorithmException, KeyStoreException {
        this.storage = storage;
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        defaultTrustManager = (X509TrustManager) factory.getTrustManagers()[0];

        reloadTrustedFingerPrints();
        this.storage.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    private void reloadTrustedFingerPrints() {
        trustedFingerprints.clear();
        Set<String> newTrustedFingerprints = storage.getTrustedFingerprints();
        log.i("Loading %d trusted fingerprints", newTrustedFingerprints.size());
        trustedFingerprints.addAll(newTrustedFingerprints);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType);
            log.d("Server is trusted by default");
        } catch (CertificateException e) {
            log.d("Server not trusted by default");

            X509Certificate serverCert = chain[0];
            String fingerprint = SSLUtils.getSha256Fingerprint(serverCert);

            if (trustedFingerprints.contains(fingerprint)) {
                log.d("Server is dynamically trusted");
                return;
            }

            log.d("Server is not yet dynamically trusted");
            throw new UntrustedCertException(fingerprint, e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        defaultTrustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void close() {
        storage.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }
}
