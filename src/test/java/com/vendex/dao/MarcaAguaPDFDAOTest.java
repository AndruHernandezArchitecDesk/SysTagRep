package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.MarcaAguaPDFConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MarcaAguaPDFDAOTest {
    private MarcaAguaPDFDAOPostgres dao;

    @BeforeEach
    void iniciarBaseTemporal() {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:marca_agua_" + java.util.UUID.randomUUID()
                + ";sql.syntax_pgs=true", "sa", "");
        DatabaseConnection.ensureMarcaAguaPDFSchema();
        dao = new MarcaAguaPDFDAOPostgres();
    }

    @AfterEach
    void restaurarBase() {
        DatabaseConnection.resetToDefault();
    }

    @Test
    void configuracionCompartidaGuardaTogglesDisenoOpacidadYLogo() throws Exception {
        byte[] logo = {1, 2, 3, 4, 5, 6};
        MarcaAguaPDFConfig expected = new MarcaAguaPDFConfig(true, false,
                MarcaAguaPDFConfig.Distribucion.REPETIDA, 0.23f, logo);

        dao.guardar(expected);

        MarcaAguaPDFConfig actual = dao.obtener();
        assertTrue(actual.isFacturaActiva());
        assertFalse(actual.isProformaActiva());
        assertEquals(MarcaAguaPDFConfig.Distribucion.REPETIDA, actual.getDistribucion());
        assertEquals(0.23f, actual.getOpacidad(), 0.001f);
        assertTrue(Arrays.equals(logo, actual.getLogoBytes()));
    }
}
