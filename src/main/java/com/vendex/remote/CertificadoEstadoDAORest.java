package com.vendex.remote;
import com.vendex.dao.CertificadoEstadoDAO;
import java.sql.Timestamp;
import java.time.LocalDate;
public class CertificadoEstadoDAORest implements CertificadoEstadoDAO {
    private final RestClient rest;
    public CertificadoEstadoDAORest(RestClient rest) { this.rest = rest; }
    public void guardarEstado(String rutaP12, String titular, LocalDate emision, LocalDate expiracion, int dias, String nivel) { try { rest.post("/certificado-estado/estado", java.util.Map.of("rutaP12",rutaP12,"titular",titular,"emision",emision,"expiracion",expiracion,"dias",dias,"nivel",nivel), Void.class); } catch (Exception e) { throw new RuntimeException(e); } }
    public Timestamp obtenerUltimoEmailEnviado(String rutaP12) { try { return rest.get("/certificado-estado/ultimo-email/"+rutaP12, Timestamp.class); } catch (Exception e) { return null; } }
    public void actualizarUltimoEmailEnviado(String rutaP12) { try { rest.put("/certificado-estado/email/"+rutaP12, null, Void.class); } catch (Exception e) { throw new RuntimeException(e); } }
    public boolean debeEnviarCorreo(String rutaP12, String nivel, Timestamp ultimoEnvio) { try { return rest.get("/certificado-estado/pendiente?ruta="+rutaP12+"&nivel="+nivel, Boolean.class); } catch (Exception e) { return false; } }
}
