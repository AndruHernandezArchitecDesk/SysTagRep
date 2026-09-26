package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.CompraResumen;
import com.vendex.model.FacturaProveedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface FacturaProveedorDAO {

    void insertar(List<FacturaProveedor> lineas);

    List<FacturaProveedor> listarFacturas(LocalDate desde, LocalDate hasta);

    boolean existeNumeroFactura(String numeroFactura, int proveedorId);

    void eliminarPorFactura(String numeroFactura, int proveedorId);

    List<CompraResumen> listarComprasPorFecha(LocalDate fecha);

    List<FacturaProveedor> listarDetallePorRango(LocalDate desde, LocalDate hasta);

    List<FacturaProveedor> listarPorFactura(String numeroFactura);

}
