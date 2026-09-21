package com.vendex.security;

/**
 * Proveedor de master key (32 bytes). Fase 1 completa.
 * Implementaciones: keyring OS o fallback file.
 */
public interface SecretProvider {
    byte[] getOrCreateMasterKey() throws Exception;
    String getName();
    boolean isAvailable();
}
