package com.vendex.security;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton que resuelve master key 32 bytes con orden: keyring OS → fallback file.
 * Cache en memoria, nunca loguea el valor.
 */
public final class MasterKeyManager {

    private static final Logger LOG = Logger.getLogger(MasterKeyManager.class.getName());
    private static volatile byte[] cached;
    private static final Object LOCK = new Object();
    private static volatile SecretProvider activeProvider;

    private MasterKeyManager() {}

    public static byte[] getMasterKey() throws Exception {
        byte[] k = cached;
        if (k != null) return k.clone();
        synchronized (LOCK) {
            if (cached != null) return cached.clone();
            // 1) keyring
            if (!Boolean.getBoolean("vendex.keyring.disabled")) {
                try {
                    KeyringSecretProvider kp = new KeyringSecretProvider();
                    if (kp.isAvailable()) {
                        byte[] key = kp.getOrCreateMasterKey();
                        cached = key.clone();
                        activeProvider = kp;
                        LOG.info("master key provista por keyring");
                        return key.clone();
                    }
                } catch (Exception e) {
                    LOG.log(Level.FINE, "keyring no disponible, usando fallback file", e);
                }
            }
            // 2) fallback file
            FallbackFileProvider fp = new FallbackFileProvider();
            byte[] key = fp.getOrCreateMasterKey();
            cached = key.clone();
            activeProvider = fp;
            LOG.info("master key provista por fallback file");
            return key.clone();
        }
    }

    public static String getActiveProviderName() {
        return activeProvider == null ? "none" : activeProvider.getName();
    }

    /** Solo para tests: resetea cache */
    public static void resetForTests() {
        synchronized (LOCK) {
            cached = null;
            activeProvider = null;
        }
    }

    /** Para tests: inyectar provider mock */
    public static void setTestMasterKey(byte[] key) {
        synchronized (LOCK) {
            cached = key.clone();
            activeProvider = new SecretProvider() {
                public byte[] getOrCreateMasterKey() { return key.clone(); }
                public String getName() { return "test"; }
                public boolean isAvailable() { return true; }
            };
        }
    }
}
