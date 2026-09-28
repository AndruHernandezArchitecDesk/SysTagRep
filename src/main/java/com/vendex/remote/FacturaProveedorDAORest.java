package com.vendex.remote;

import com.vendex.dao.FacturaProveedorDAO;
import com.vendex.model.CompraResumen;
import com.vendex.model.FacturaProveedor;
import java.util.List;
import java.time.LocalDate;
import java.lang.String;

public class FacturaProveedorDAORest extends RestDao<FacturaProveedor> implements FacturaProveedorDAO {
    public FacturaProveedorDAORest(RestClient rest) { super(rest, "/facturas-proveedor", FacturaProveedor.class); }
    public void insertar(List lineas) { throw new UnsupportedOperationException("FacturaProveedor.insertar rest"); }
    public List<FacturaProveedor> listarFacturas(LocalDate desde, LocalDate hasta) { throw new UnsupportedOperationException("FacturaProveedor.listarFacturas rest"); }
    public boolean existeNumeroFactura(String numeroFactura, int proveedorId) { throw new UnsupportedOperationException("FacturaProveedor.existeNumeroFactura rest"); }
    public void eliminarPorFactura(String numeroFactura, int proveedorId) { throw new UnsupportedOperationException("FacturaProveedor.eliminarPorFactura rest"); }
    public List<CompraResumen> listarComprasPorFecha(LocalDate fecha) { throw new UnsupportedOperationException("FacturaProveedor.listarComprasPorFecha rest"); }
    public List<FacturaProveedor> listarDetallePorRango(LocalDate desde, LocalDate hasta) { throw new UnsupportedOperationException("FacturaProveedor.listarDetallePorRango rest"); }
    public List<FacturaProveedor> listarPorFactura(String numeroFactura) { throw new UnsupportedOperationException("FacturaProveedor.listarPorFactura rest"); }
}