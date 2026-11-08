package com.vendex.dao;

import com.vendex.exception.SecretoNoEncontradoException;
import com.vendex.model.ConfiguracionEmail;
import com.vendex.security.InstallMasterKeyManager;
import com.vendex.security.MasterKeyManager;
import com.vendex.util.Cifrado;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracionEmailCifradoTest {
    @Test
    void correoLegacySeDescifraAunqueFalteClaveDeInstalacion() throws Exception {
        try (MockedStatic<InstallMasterKeyManager> install = mockStatic(InstallMasterKeyManager.class);
             MockedStatic<MasterKeyManager> local = mockStatic(MasterKeyManager.class)) {
            install.when(InstallMasterKeyManager::obtenerMasterKeyBytes)
                    .thenThrow(new SecretoNoEncontradoException("vendex-master-key"));
            local.when(MasterKeyManager::getMasterKey).thenReturn(new byte[32]);
            ConfiguracionEmail cfg = new ConfiguracionEmail();
            cfg.setPasswordCifrado(Cifrado.encriptarLegacy("smtp-legacy"));
            assertEquals("smtp-legacy", new ConfiguracionEmailDAOPostgres().obtenerPasswordPlano(cfg));
            cfg.setPasswordCifrado(Cifrado.encriptar("smtp-local"));
            assertEquals("smtp-local", new ConfiguracionEmailDAOPostgres().obtenerPasswordPlano(cfg));
        }
    }

    @Test
    void claveCompartidaSePuedeValidarAntesDeImportarYRechazaOtraClave() throws Exception {
        byte[] correcta = new byte[32];
        byte[] incorrecta = new byte[32];
        incorrecta[0] = 1;
        try (MockedStatic<InstallMasterKeyManager> install = mockStatic(InstallMasterKeyManager.class);
             MockedStatic<MasterKeyManager> local = mockStatic(MasterKeyManager.class)) {
            install.when(InstallMasterKeyManager::obtenerMasterKeyBytes).thenReturn(correcta);
            local.when(MasterKeyManager::getMasterKey).thenReturn(incorrecta);
            String cifrada = ConfiguracionEmailDAOPostgres.cifrarParaGuardar("smtp-compartido");
            assertEquals("smtp-compartido", Cifrado.desencriptarConInstallMaster(cifrada, correcta));
            assertThrows(Exception.class, () -> Cifrado.desencriptarConInstallMaster(cifrada, incorrecta));
            install.when(InstallMasterKeyManager::obtenerMasterKeyBytes)
                    .thenThrow(new SecretoNoEncontradoException("vendex-master-key"));
            ConfiguracionEmail cfg = new ConfiguracionEmail();
            cfg.setPasswordCifrado(cifrada);
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> new ConfiguracionEmailDAOPostgres().obtenerPasswordPlano(cfg));
            assertTrue(error.getMessage().contains("importa la clave"));
            assertThrows(IllegalStateException.class,
                    () -> ConfiguracionEmailDAOPostgres.cifrarParaGuardar("otra"));
        }
    }
}
