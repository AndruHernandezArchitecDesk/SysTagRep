package com.vendex.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LicenseExpiryAlertTest {
    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);

    @Test
    void avisaEnElLimiteDeDiezDiasYElDiaDeVencimiento() {
        assertEquals(10, LicenseExpiryAlert.diasRestantes(licencia(HOY.plusDays(10), true, false), HOY).orElseThrow());
        assertEquals(0, LicenseExpiryAlert.diasRestantes(licencia(HOY, true, false), HOY).orElseThrow());
        assertTrue(LicenseExpiryAlert.mensaje(licencia(HOY, true, false), 0).contains("vence hoy"));
    }

    @Test
    void noAvisaFueraDeVentanaNiParaLicenciasInvalidasOVitalicias() {
        assertTrue(LicenseExpiryAlert.diasRestantes(licencia(HOY.plusDays(11), true, false), HOY).isEmpty());
        assertTrue(LicenseExpiryAlert.diasRestantes(licencia(HOY.minusDays(1), true, false), HOY).isEmpty());
        assertTrue(LicenseExpiryAlert.diasRestantes(licencia(HOY.plusDays(2), false, false), HOY).isEmpty());
        assertTrue(LicenseExpiryAlert.diasRestantes(licencia(null, true, true), HOY).isEmpty());
        assertTrue(LicenseExpiryAlert.diasRestantes(null, HOY).isEmpty());
    }

    private static LicenseManager.LicenseInfo licencia(LocalDate vencimiento, boolean valida, boolean perpetua) {
        return new LicenseManager.LicenseInfo(valida, true, perpetua, vencimiento,
                LicenseManager.MODALIDAD_CENTRAL, true, Set.of(), 0);
    }
}
