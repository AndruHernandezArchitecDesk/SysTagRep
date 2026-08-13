package com.tag.sysTagRep.util;

import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.model.Empresa;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Genera la Clave de Acceso de 49 dígitos para comprobantes electrónicos del SRI.
 * Formato: ddmmaaaa | tipocomprobante | ruc | ambiente | serie(6) | secuencial(9) | codigonumerico(8) | tipoemision(1) | digitoverificador(1)
 */
public class ClaveAcceso {

    public static String generar(String tipoComprobante, String ruc, String ambiente,
                                  String establecimiento, String puntoEmision, int secuencial) {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String tipoComp = String.format("%02d", Integer.parseInt(tipoComprobante));
        String amb = ambiente.equals("PRODUCCION") ? "2" : "1"; // 1=PRUEBAS, 2=PRODUCCION
        String serie = establecimiento + puntoEmision;
        String sec = String.format("%09d", secuencial);
        String codigoNumerico = String.format("%08d", (int) (Math.random() * 99999999));
        String tipoEmision = "1";

        String base = fecha + tipoComp + ruc + amb + serie + sec + codigoNumerico + tipoEmision;
        int digitoVerificador = calcularDigitoVerificador(base);

        return base + digitoVerificador;
    }

    private static int calcularDigitoVerificador(String base) {
        // Módulo 11 del SRI: pesos 2,3,4,5,6,7 aplicados de derecha a izquierda,
        // sin reducción de dígitos (no es módulo 10).
        int[] factores = {2, 3, 4, 5, 6, 7};
        int suma = 0;
        int longitud = base.length();

        for (int i = 0; i < longitud; i++) {
            int digito = Character.getNumericValue(base.charAt(longitud - 1 - i));
            suma += digito * factores[i % factores.length];
        }

        int residuo = suma % 11;
        int digito = 11 - residuo;
        if (digito == 11) return 0;
        if (digito == 10) return 1;
        return digito;
    }

    public static String getUrlConsulta(String claveAcceso) {
        return "https://celcerce.prib.nubefact.com/cvc-ws/validarcomprobante?claveAcceso=" + claveAcceso;
    }
}
