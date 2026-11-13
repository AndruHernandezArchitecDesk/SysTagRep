package com.vendex.model;

/** Configuración compartida de marca de agua para facturas y proformas PDF. */
public final class MarcaAguaPDFConfig {
    public enum Distribucion { CENTRO, REPETIDA }

    private final boolean facturaActiva;
    private final boolean proformaActiva;
    private final Distribucion distribucion;
    private final float opacidad;
    private final byte[] logoBytes;

    public MarcaAguaPDFConfig(boolean facturaActiva, boolean proformaActiva,
                              Distribucion distribucion, float opacidad, byte[] logoBytes) {
        this.facturaActiva = facturaActiva;
        this.proformaActiva = proformaActiva;
        this.distribucion = distribucion == null ? Distribucion.CENTRO : distribucion;
        this.opacidad = Math.max(0.05f, Math.min(0.40f, opacidad));
        this.logoBytes = logoBytes == null ? null : logoBytes.clone();
    }

    public static MarcaAguaPDFConfig desactivada() {
        return new MarcaAguaPDFConfig(false, false, Distribucion.CENTRO, 0.12f, null);
    }

    public boolean isFacturaActiva() { return facturaActiva; }
    public boolean isProformaActiva() { return proformaActiva; }
    public Distribucion getDistribucion() { return distribucion; }
    public float getOpacidad() { return opacidad; }
    public byte[] getLogoBytes() { return logoBytes == null ? null : logoBytes.clone(); }
    public boolean isActivaParaFactura() { return facturaActiva; }
    public boolean isActivaParaProforma() { return proformaActiva; }
}
