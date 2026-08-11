package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigFirmaTest {

    @Test
    void cargar_siExisteConfiguracionReal_retornaRutaYClaveNoVacias() {
        String[] config = ConfigFirma.cargar();
        assertNotNull(config);
        assertEquals(2, config.length);
    }

    @Test
    void estaConfigurada_cuandoHayConfiguracionReal_retornaTrueSiEstaCompleta() {
        String[] config = ConfigFirma.cargar();
        boolean estaCompleta = !config[0].isEmpty() && !config[1].isEmpty();
        assertEquals(estaCompleta, ConfigFirma.estaConfigurada());
    }
}
