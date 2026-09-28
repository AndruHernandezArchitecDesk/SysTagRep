package com.vendex.remote;

import com.vendex.dao.ComprobanteTempDAO;
import com.vendex.model.ComprobanteTemp;
import java.util.List;
import java.lang.String;

public class ComprobanteTempDAORest extends RestDao<ComprobanteTemp> implements ComprobanteTempDAO {
    public ComprobanteTempDAORest(RestClient rest) { super(rest, "/comprobantes-temp", ComprobanteTemp.class); }
    public int insertar(int proformaId, String codigo, String descripcion, int cantidad, java.math.BigDecimal precioUnitario, java.math.BigDecimal precioTotal) { throw new UnsupportedOperationException("ComprobanteTemp.insertar rest"); }
    public List<ComprobanteTemp> listarPorProforma(int proformaId) { throw new UnsupportedOperationException("ComprobanteTemp.listarPorProforma rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("ComprobanteTemp.eliminar rest"); }
    public void limpiarPorProforma(int proformaId) { throw new UnsupportedOperationException("ComprobanteTemp.limpiarPorProforma rest"); }
}