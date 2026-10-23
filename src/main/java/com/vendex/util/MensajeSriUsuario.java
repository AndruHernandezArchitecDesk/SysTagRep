package com.vendex.util;

import java.text.Normalizer;
import java.util.Locale;

/** Explica el estado del comprobante con prudencia y deja el mensaje original intacto. */
public final class MensajeSriUsuario {
    private MensajeSriUsuario() {}

    public static String explicar(String estado, String mensajeSri) {
        String estadoNormalizado = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        String original = mensajeSri == null ? "" : mensajeSri.trim();
        String texto = normalizar(original);

        if (estadoNormalizado.equals("ANULADO"))
            return "Se detuvieron los reintentos localmente. Esto no anula el comprobante ante el SRI.";
        if (texto.contains("firma no configurada") || texto.contains("no se configuro la firma"))
            return "No se pudo enviar: falta configurar la firma electrónica.";
        if (texto.contains("error de conexion") || texto.contains("sin conexion") || texto.contains("timeout")
                || texto.contains("no hubo respuesta del sri"))
            return "No se pudo confirmar la respuesta del SRI por un problema de conexión.";
        if (estadoNormalizado.equals("AUTORIZADO"))
            return "El SRI autorizó el comprobante.";
        if (estadoNormalizado.equals("RECHAZADA") || estadoNormalizado.equals("NO AUTORIZADO"))
            return "El SRI rechazó el comprobante. Revisa el detalle para corregir los datos señalados.";
        if (estadoNormalizado.equals("DEVUELTA") || texto.contains("devuelta") || texto.contains("devuelto"))
            return "El SRI devolvió el comprobante. Revisa el detalle técnico para corregirlo y volver a enviarlo.";
        if (estadoNormalizado.equals("AGOTADA"))
            return "Se agotaron los reintentos automáticos. Revisa el detalle y decide si corresponde corregir y reenviar.";
        if (estadoNormalizado.equals("PENDIENTE") || estadoNormalizado.equals("RECIBIDA")) {
            if (texto.contains("recibido") || texto.contains("recibida"))
                return "El SRI recibió el comprobante; todavía no confirma la autorización.";
            if (original.isEmpty()) return "El SRI todavía no confirma la autorización; se seguirá consultando.";
            return "El comprobante sigue pendiente de confirmación del SRI.";
        }
        if (estadoNormalizado.equals("ERROR"))
            return "Ocurrió un error al consultar o enviar el comprobante. Revisa el detalle técnico.";
        if (original.isEmpty()) return "No hay información adicional del SRI.";
        return "El SRI informó una observación. Consulta el detalle técnico para conocerla.";
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
