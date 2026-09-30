package com.vendex.remote;

import com.vendex.dao.SecuenciaDocumentoDAO;
import com.vendex.model.SecuenciaDocumento;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;

public class SecuenciaDocumentoDAORest extends RestDao<SecuenciaDocumento> implements SecuenciaDocumentoDAO {

    public SecuenciaDocumentoDAORest(RestClient rest) {
        super(rest, "/secuencia-documento", SecuenciaDocumento.class);
    }

    @Override
    public SecuenciaDocumento obtener(int puntoEmisionId, String tipo) {
        try {
            return rest.get("/api/secuencia-documento/" + puntoEmisionId + "/" + URLEncoder.encode(tipo, StandardCharsets.UTF_8), SecuenciaDocumento.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean establecer(int puntoEmisionId, String tipo, String prefijo, String establecimiento, String puntoEmision, int numero) {
        throw new UnsupportedOperationException("SecuenciaDocumento.establecer rest");
    }

    @Override
    public int marcarUsado(int puntoEmisionId, String tipo) {
        try {
            Map<String, Object> resp = rest.post("/api/secuencia-documento/" + puntoEmisionId + "/" + URLEncoder.encode(tipo, StandardCharsets.UTF_8) + "/marcar-usado", Map.of(), Map.class);
            if (resp != null && resp.get("secuencial") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int marcarUsado(Connection con, int puntoEmisionId, String tipo) {
        return marcarUsado(puntoEmisionId, tipo);
    }

    @Override
    public SecuenciaDocumento obtener(Connection con, int puntoEmisionId, String tipo) {
        return obtener(puntoEmisionId, tipo);
    }

    @Override
    public boolean existeCodigoNotaVenta(String codigo) {
        throw new UnsupportedOperationException("SecuenciaDocumento.existeCodigoNotaVenta rest");
    }

    @Override
    public boolean existeCodigoFactura(String codigo) {
        throw new UnsupportedOperationException("SecuenciaDocumento.existeCodigoFactura rest");
    }

    @Override
    public boolean existeCodigoNotaCredito(String codigo) {
        throw new UnsupportedOperationException("SecuenciaDocumento.existeCodigoNotaCredito rest");
    }
}
