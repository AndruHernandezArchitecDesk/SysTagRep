package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class ConfigAmbienteTest {

    @Test
    void cargar_sinArchivo_retornaPruebasPorDefecto() {
        String ambiente = ConfigAmbiente.cargar();
        assertEquals("PRUEBAS", ambiente);
    }

    @Test
    void guardarYCargar_ambienteProduccion_persiste() {
        ConfigAmbiente.guardar("PRODUCCION");
        assertEquals("PRODUCCION", ConfigAmbiente.cargar());
        ConfigAmbiente.guardar("PRUEBAS");
    }

    @Test
    void guardarYCargar_ambientePruebas_persiste() {
        ConfigAmbiente.guardar("PRUEBAS");
        assertEquals("PRUEBAS", ConfigAmbiente.cargar());
    }

    @Test
    void guardar_null_guardaPruebasComoDefecto() {
        ConfigAmbiente.guardar(null);
        assertEquals("PRUEBAS", ConfigAmbiente.cargar());
    }
}
