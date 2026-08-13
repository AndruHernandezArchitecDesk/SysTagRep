package com.tag.sysTagRep.util;

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
}
