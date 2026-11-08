package com.vendex.config;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

class FlywayClienteTest {
    @Test
    void clienteRemotoNoLeeCredencialesNiIntentaMigrar() {
        try (MockedStatic<DbConfig> config = mockStatic(DbConfig.class);
             MockedStatic<PostgresConfig> postgres = mockStatic(PostgresConfig.class)) {
            config.when(DbConfig::esRemota).thenReturn(true);
            DatabaseConnection.migrateWithFlywayIfAvailable();
            postgres.verifyNoInteractions();
            config.verify(DbConfig::esRemota);
            config.verifyNoMoreInteractions();
        }
    }

    @Test
    void hostCentralSinPasswordConservaFallback() {
        try (MockedStatic<DbConfig> config = mockStatic(DbConfig.class);
             MockedStatic<PostgresConfig> postgres = mockStatic(PostgresConfig.class)) {
            config.when(DbConfig::esRemota).thenReturn(false);
            postgres.when(PostgresConfig::getPassword).thenReturn(null);
            DatabaseConnection.migrateWithFlywayIfAvailable();
            postgres.verify(PostgresConfig::getPassword);
            config.verify(DbConfig::esRemota);
            config.verifyNoMoreInteractions();
        }
    }
}
