package com.vendex.remote;

import com.vendex.dao.SecuenciaDocumentoDAO;
import com.vendex.model.SecuenciaDocumento;
import java.sql.Connection;
import java.lang.String;

public class SecuenciaDocumentoDAORest extends RestDao<SecuenciaDocumento> implements SecuenciaDocumentoDAO {
    public SecuenciaDocumentoDAORest(RestClient rest) { super(rest, "/secuencia-documento", SecuenciaDocumento.class); }
    public SecuenciaDocumento obtener(int puntoEmisionId, String tipo) { throw new UnsupportedOperationException("SecuenciaDocumento.obtener rest"); }
    public boolean establecer(int puntoEmisionId, String tipo, String prefijo, String establecimiento, String puntoEmision, int numero) { throw new UnsupportedOperationException("SecuenciaDocumento.establecer rest"); }
    public int marcarUsado(int puntoEmisionId, String tipo) { throw new UnsupportedOperationException("SecuenciaDocumento.marcarUsado rest"); }
    public int marcarUsado(Connection con, int puntoEmisionId, String tipo) { throw new UnsupportedOperationException("SecuenciaDocumento.marcarUsado rest"); }
    public SecuenciaDocumento obtener(Connection con, int puntoEmisionId, String tipo) { throw new UnsupportedOperationException("SecuenciaDocumento.obtener rest"); }
    public boolean existeCodigoNotaVenta(String codigo) { throw new UnsupportedOperationException("SecuenciaDocumento.existeCodigoNotaVenta rest"); }
    public boolean existeCodigoFactura(String codigo) { throw new UnsupportedOperationException("SecuenciaDocumento.existeCodigoFactura rest"); }
    public boolean existeCodigoNotaCredito(String codigo) { throw new UnsupportedOperationException("SecuenciaDocumento.existeCodigoNotaCredito rest"); }
}