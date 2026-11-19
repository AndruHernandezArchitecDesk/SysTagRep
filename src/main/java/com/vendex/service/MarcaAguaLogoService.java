package com.vendex.service;

import com.vendex.dao.MarcaAguaPDFDAO;
import com.vendex.model.MarcaAguaPDFConfig;
import com.vendex.util.MarcaAguaPDF;

/** Sincroniza el logo de Mi Empresa con la configuración central de marcas de agua. */
public final class MarcaAguaLogoService {
    private final MarcaAguaPDFDAO dao;

    public MarcaAguaLogoService(MarcaAguaPDFDAO dao) {
        this.dao = java.util.Objects.requireNonNull(dao, "dao");
    }

    public void sincronizar(byte[] logoBytes) throws Exception {
        byte[] logoNormalizado = MarcaAguaPDF.normalizarLogo(logoBytes);
        MarcaAguaPDFConfig actual = dao.obtener();
        if (actual == null) actual = MarcaAguaPDFConfig.desactivada();
        dao.guardar(new MarcaAguaPDFConfig(actual.isFacturaActiva(), actual.isProformaActiva(),
                actual.getDistribucion(), actual.getOpacidad(), logoNormalizado));
    }
}
