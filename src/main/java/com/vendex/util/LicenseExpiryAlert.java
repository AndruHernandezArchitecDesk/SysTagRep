package com.vendex.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.OptionalLong;

/** Regla de aviso de vencimiento de licencia, comprobada una vez por arranque. */
public final class LicenseExpiryAlert {
    public static final long DIAS_ANTES = 10;

    private LicenseExpiryAlert() { }

    /** Devuelve días restantes solo si la licencia válida vence dentro de la ventana de aviso. */
    public static OptionalLong diasRestantes(LicenseManager.LicenseInfo licencia, LocalDate hoy) {
        if (licencia == null || !licencia.valida || licencia.perpetua || licencia.vencimiento == null || hoy == null) {
            return OptionalLong.empty();
        }
        long dias = ChronoUnit.DAYS.between(hoy, licencia.vencimiento);
        return dias >= 0 && dias <= DIAS_ANTES ? OptionalLong.of(dias) : OptionalLong.empty();
    }

    public static String mensaje(LicenseManager.LicenseInfo licencia, long diasRestantes) {
        String fecha = licencia.vencimiento.toString();
        if (diasRestantes == 0) {
            return "La licencia de Vendex vence hoy (" + fecha + "). Contacta al administrador para renovarla.";
        }
        String unidad = diasRestantes == 1 ? "día" : "días";
        return "La licencia de Vendex vence el " + fecha + ". Quedan " + diasRestantes + " " + unidad
                + ". Contacta al administrador para renovarla.";
    }
}
