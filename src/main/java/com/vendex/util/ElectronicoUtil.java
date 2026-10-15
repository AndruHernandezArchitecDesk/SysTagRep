package com.vendex.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilidades del flujo de facturación electrónica.
 */
public final class ElectronicoUtil {

    private ElectronicoUtil() {}

    /**
     * Indica si corresponde notificar al cliente por correo un comprobante
     * electrónico: únicamente cuando el SRI lo autorizó y se dispone del
     * número y fecha de autorización.
     */
    public static boolean debeEnviarNotificacion(String estadoSri, String numeroAutorizacion, String fechaAutorizacion) {
        return AppConstants.ESTADO_AUTORIZADO.equals(estadoSri)
                && numeroAutorizacion != null && !numeroAutorizacion.trim().isEmpty()
                && fechaAutorizacion != null && !fechaAutorizacion.trim().isEmpty();
    }

    /**
     * Resuelve el número de autorización a mostrar/imprimir.
     * En SRI offline AUTORIZADO implica numero==claveAcceso, así que si el
     * número viene vacío pero la clave es válida (49 dígitos) se usa la clave.
     * Sin red ni consultas: dato deducible del propio comprobante.
     */
    public static String resolverNumero(String claveAcceso, String numeroAutorizacion) {
        if (numeroAutorizacion != null && !numeroAutorizacion.trim().isEmpty()) return numeroAutorizacion;
        if (claveAcceso != null && claveAcceso.trim().matches("\\d{49}")) return claveAcceso.trim();
        return numeroAutorizacion;
    }

    /** Mensaje por defecto al rescatar un AUTORIZADO sin mensaje guardado. */
    public static String resolverMensaje(String mensajeSri) {
        if (mensajeSri != null && !mensajeSri.trim().isEmpty()) return mensajeSri;
        return "Autorizado SRI";
    }

    /**
     * Precio sin IVA a 2 decimales: el que viaja en el XML (precioUnitario) y base
     * única de todos los cálculos. Redondear una sola vez evita el doble redondeo
     * (precio a 6 dec para el total pero a 2 dec para el XML) que descuadraba
     * cantidad*precio-descuento vs total en 1 centavo y el SRI rechaza.
     */
    public static BigDecimal precioSinIva2(BigDecimal precioConIva) {
        if (precioConIva == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return precioConIva.divide(new BigDecimal("1.15"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Neto de línea sin IVA: cantidad × precioSinIva2 − descuento, a 2 decimales.
     * Debe usarse para el total de línea (f[5]) y para suma de bases, de modo que
     * el XML (precioUnitario=f[3], precioTotalSinImpuesto=f[5]) siempre cumpla
     * cantidad*precio-descuento == total.
     */
    public static BigDecimal netoLinea(BigDecimal cantidad, BigDecimal precioSinIva2, BigDecimal descuento) {
        BigDecimal c = cantidad != null ? cantidad : BigDecimal.ZERO;
        BigDecimal p = precioSinIva2 != null ? precioSinIva2 : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal d = descuento != null ? descuento : BigDecimal.ZERO;
        return p.multiply(c).subtract(d).setScale(2, RoundingMode.HALF_UP);
    }

    /** Resultado del rescate de autorización para un AUTORIZADO sin número en BD. */
    public static final class AutRescatada {
        public final String numero;
        public final String mensaje;
        public final String fecha;
        public final boolean rescatado;
        public AutRescatada(String numero, String mensaje, String fecha, boolean rescatado) {
            this.numero = numero; this.mensaje = mensaje; this.fecha = fecha; this.rescatado = rescatado;
        }
    }

    private static boolean vacio(String s) { return s == null || s.trim().isEmpty(); }

    /**
     * Calcula número/mensaje/fecha a usar cuando el registro está AUTORIZADO pero
     * sin número en BD. Solo marca rescatado=true si falta el número (el caso real
     * de inconsistencia); el llamador persiste en registro + comprobantes.
     */
    public static AutRescatada rescatarAutorizacion(String claveAcceso, String estadoSri,
                                                   String mensajeReg, String numeroReg, String numeroParam,
                                                   String fechaReg, String fechaParam) {
        String numParam = !vacio(numeroParam) ? numeroParam : null;
        String fecParam = !vacio(fechaParam) ? fechaParam : null;
        if (!AppConstants.ESTADO_AUTORIZADO.equals(estadoSri) || !vacio(numeroReg)) {
            return new AutRescatada(numParam != null ? numParam : numeroReg, mensajeReg,
                    fecParam != null ? fecParam : fechaReg, false);
        }
        String num = resolverNumero(claveAcceso, numParam != null ? numParam : numeroReg);
        String msg = vacio(mensajeReg) ? "Autorizado SRI" : mensajeReg;
        String fec = fecParam != null ? fecParam : fechaReg;
        return new AutRescatada(num, msg, fec, true);
    }
}
