# Activación del ambiente PRUEBAS (certificación) en el SRI

Para que el SRI autorice comprobantes en el ambiente de pruebas (y la factura
muestre número y fecha/hora de autorización), el RUC 1716854540001 debe tener
habilitado el ambiente de **Pruebas / Certificación** en el portal del SRI.

## Pasos (en SRI en Línea)

1. Ingresar a [www.sri.gob.ec](https://www.sri.gob.ec) y entrar a **SRI en Línea**.
2. Ir a **Facturación electrónica**.
3. Seleccionar la opción **Pruebas**.
4. Seleccionar **Autorización**.
5. Completar la **Solicitud de emisión** (clic en "Siguiente" y confirmar).
6. Esperar el oficio de aprobación (PDF) en el buzón del contribuyente.

## Contexto

- La aplicación SysTagRep ya envía los comprobantes al ambiente PRUEBAS
  correctamente: `<ambiente>2</ambiente>`, clave de acceso con dígito de
  ambiente `2`, y el WS de recepción/autorización `celcer.sri.gob.ec`.
- Si el SRI devuelve *"El ambiente de la solicitud PRODUCCIÓN no coincide con
  el de ejecución PRUEBAS"*, significa que el RUC está registrado como
  PRODUCCIÓN y el ambiente PRUEBAS aún no está activado para ese RUC.
- Alternativa mientras tanto: emitir en **PRODUCCIÓN** (el SRI autoriza de
  inmediato, pero los comprobantes tienen validez tributaria real).
- Los comprobantes autorizados en PRUEBAS devuelven un número y fecha de
  autorización de prueba (sin validez tributaria).
