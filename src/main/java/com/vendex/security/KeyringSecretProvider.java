package com.vendex.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keyring OS via java-keyring (DPAPI Windows / Keychain macOS / Secret Service Linux).
 * Almacena master key 32 bytes base64 en servicio "Vendex" cuenta "master-key".
 * Si keyring no disponible (headless, CI, Linux sin libsecret) lanza y caller cae a fallback.
 * Usa timeout 5s para no bloquear MainApp.start() si el keyring pide consentimiento interactivo.
 */
public class KeyringSecretProvider implements SecretProvider {

    private static final Logger LOG = Logger.getLogger(KeyringSecretProvider.class.getName());
    private static final String SERVICE = "Vendex";
    private static final String ACCOUNT = "master-key";
    private static final int KEY_BYTES = 32;

    @Override
    public String getName() { return "keyring"; }

    @Override
    public boolean isAvailable() {
        if (Boolean.getBoolean("vendex.keyring.disabled")) return false;
        String os = System.getProperty("os.name", "").toLowerCase();
        // fallback es suficiente en Linux (decisión 2); solo usar keyring en Windows/macOS donde es estable
        if (os.contains("linux")) return false;
        return os.contains("win") || os.contains("mac") || os.contains("darwin");
    }

    @Override
    public byte[] getOrCreateMasterKey() throws Exception {
        if (!isAvailable()) throw new UnsupportedOperationException("keyring disabled");
        Callable<byte[]> task = () -> {
            // reflection para no hard-fail si java-keyring no está en classpath (tests sin deps)
            try {
                Class<?> keyringClass = Class.forName("com.github.javakeyring.Keyring");
                Object keyring = keyringClass.getMethod("create").invoke(null);
                // getPassword(service, account)
                String existing = null;
                try {
                    existing = (String) keyringClass.getMethod("getPassword", String.class, String.class)
                            .invoke(keyring, SERVICE, ACCOUNT);
                } catch (Exception e) {
                    // com.github.javakeyring.PasswordAccessException o similar -> tratar como no existe
                    LOG.log(Level.FINE, "keyring getPassword no existe o error", e);
                }
                if (existing != null && !existing.isBlank()) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(existing.trim());
                        if (decoded.length == KEY_BYTES) return decoded;
                        LOG.warning("keyring master key longitud inesperada, regenerando");
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "keyring master key base64 corrupta, regenerando", e);
                    }
                }
                // generar nueva
                byte[] key = new byte[KEY_BYTES];
                new SecureRandom().nextBytes(key);
                String b64 = Base64.getEncoder().encodeToString(key);
                keyringClass.getMethod("setPassword", String.class, String.class, String.class)
                        .invoke(keyring, SERVICE, ACCOUNT, b64);
                LOG.info("keyring master key generada y guardada en " + SERVICE + "/" + ACCOUNT);
                return key;
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException("java-keyring no disponible", e);
            }
        };
        ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "vendex-keyring");
            t.setDaemon(true);
            return t;
        });
        Future<byte[]> fut = exec.submit(task);
        try {
            return fut.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fut.cancel(true);
            throw new TimeoutException("keyring timeout 5s (posible prompt interactivo)");
        } finally {
            exec.shutdownNow();
        }
    }
}
