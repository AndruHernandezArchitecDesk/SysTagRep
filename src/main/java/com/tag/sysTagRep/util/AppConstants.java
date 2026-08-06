package com.tag.sysTagRep.util;

import java.math.BigDecimal;

public final class AppConstants {

    private AppConstants() {}

    public static final String ESTABLECIMIENTO_DEFAULT = "001";
    public static final String PUNTO_EMISION_DEFAULT = "001";
    public static final String TIPO_COMPROBANTE_FACTURA = "01";
    public static final String NOMBRE_DOCUMENTO_FACTURA = "FACTURA";
    public static final String TIPO_DOCUMENTO_FACTURA = "FACTURA";

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_AUTORIZADO = "AUTORIZADO";
    public static final String ESTADO_RECHAZADA = "RECHAZADA";
    public static final String ESTADO_DEVUELTA = "DEVUELTA";
    public static final String ESTADO_ERROR = "ERROR";
    public static final String ESTADO_RECIBIDA = "RECIBIDA";

    public static final String AMBIENTE_PRUEBAS = "PRUEBAS";
    public static final String AMBIENTE_PRODUCCION = "PRODUCCION";

    public static final String FORMA_PAGO_CREDITO = "TAG Crédito";

    public static final BigDecimal IVA_RATE = new BigDecimal("0.15");
    public static final BigDecimal CIEN = new BigDecimal("100");
    public static final BigDecimal CERO = BigDecimal.ZERO;
    public static final BigDecimal DESCUENTO_POR_DEFECTO = BigDecimal.ZERO;

    public static final int[] MESES_PLAZO = {5, 10, 15, 20, 25, 30};
    public static final String[] TASAS_INTERES = {"0", "3", "6", "9", "12", "15"};
    public static final int PASO_DESCUENTO = 5;

    public static final int MAX_DESCRIPCION_XML = 99;
    public static final int MAX_LONGITUD_IDENTIFICACION_JURIDICA = 13;

    public static final String ASUNTO_FACTURA = "FACTURA";
    public static final String ASUNTO_PROFORMA = "PROFORMA";

    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final int SMTP_PORT = 587;
    public static final int SMTP_TIMEOUT_MS = 15000;

    public static final String DIRECTORIO_ESCRITORIO_DEFAULT = "Desktop";
    public static final String DIRECTORIO_ESCRITORIO_ALT = "Escritorio";

    public static final String PATRON_FECHA_EMISION = "dd/MM/yyyy";
    public static final String PATRON_NUMERO_COMPROBANTE = "%07d";

    public static final String EXTENSION_PDF = ".pdf";
    public static final String EXTENSION_XML = ".xml";
    public static final String PREFIJO_PDF_FACTURA = "FacturaElectronica_";

    public static final String ESTADO_SIN_ENVIO = "NO ENVIADO";
    public static final String ESTADO_ERROR_CONEXION = "ERROR DE CONEXIÓN";
}
