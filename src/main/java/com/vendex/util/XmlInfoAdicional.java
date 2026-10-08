package com.vendex.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Campos comunes de infoAdicional para todos los comprobantes electrónicos.
 * Resolución NAC-DGERCGC26-00000027: todo comprobante emitido con software de
 * terceros debe incluir el RUC del proveedor del sistema en infoAdicional.
 */
public final class XmlInfoAdicional {

    /** Nombre exacto del campo según Ficha Técnica SRI. */
    public static final String NOMBRE_CAMPO_RUC_PROVEEDOR = "RUC Proveedor";

    private XmlInfoAdicional() {}

    /**
     * Agrega el campo RUC Proveedor al bloque infoAdicional ya creado.
     */
    public static void agregarRucProveedor(Document doc, Element infoAdicional) {
        Element campoProv = doc.createElement("campoAdicional");
        campoProv.setAttribute("nombre", NOMBRE_CAMPO_RUC_PROVEEDOR);
        campoProv.setTextContent(AppConstants.RUC_PROVEEDOR_SISTEMA);
        infoAdicional.appendChild(campoProv);
    }
}
