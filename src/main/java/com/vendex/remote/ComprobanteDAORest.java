package com.vendex.remote;

import com.vendex.dao.ComprobanteDAO;
import java.sql.Connection;
import java.lang.Integer;
import java.lang.String;

public class ComprobanteDAORest implements ComprobanteDAO {
    private final RestClient rest;
    public ComprobanteDAORest(RestClient rest) { this.rest = rest; }
    public int obtenerSecuencial(String tipoComprobante) { throw new UnsupportedOperationException("Comprobante.obtenerSecuencial"); }
    public void insertar(String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado) { try { rest.post("/comprobantes/insertar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void insertar(String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado, String tipoComprobante) { try { rest.post("/comprobantes/insertar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void insertar(Connection con, String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado, String tipoComprobante) { try { rest.post("/comprobantes/insertar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void insertar(Connection con, String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado) { try { rest.post("/comprobantes/insertar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String xmlAutorizado, String numeroAutorizacion, String fechaAutorizacion) { try { rest.post("/comprobantes/actualizarEstado", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String xmlAutorizado, String numeroAutorizacion, String fechaAutorizacion) { try { rest.post("/comprobantes/actualizarEstado", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void guardarEnvio(String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { try { rest.post("/comprobantes/guardarEnvio", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void guardarEnvio(String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion, String tipoComprobante) { try { rest.post("/comprobantes/guardarEnvio", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void guardarEnvio(Connection con, String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion, String tipoComprobante) { try { rest.post("/comprobantes/guardarEnvio", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void guardarEnvio(Connection con, String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { try { rest.post("/comprobantes/guardarEnvio", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public int consultarSecuencial(String tipoComprobante) { throw new UnsupportedOperationException("Comprobante.consultarSecuencial"); }
}