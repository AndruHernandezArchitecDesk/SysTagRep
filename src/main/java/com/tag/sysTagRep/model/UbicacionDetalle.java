package com.tag.sysTagRep.model;

public class UbicacionDetalle {
    private int id;
    private String codigoUbicacion;
    private int idPerchero;
    private String nombrePerchero;
    private String seccion;
    private String estado;
    private Integer idProducto;
    private String productoDescripcion;
    private String productoCodigo;
    private int cantidad;
    private String grupoNombre;
    private String marcaNombre;

    public UbicacionDetalle() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigoUbicacion() { return codigoUbicacion; }
    public void setCodigoUbicacion(String codigoUbicacion) { this.codigoUbicacion = codigoUbicacion; }

    public int getIdPerchero() { return idPerchero; }
    public void setIdPerchero(int idPerchero) { this.idPerchero = idPerchero; }

    public String getNombrePerchero() { return nombrePerchero; }
    public void setNombrePerchero(String nombrePerchero) { this.nombrePerchero = nombrePerchero; }

    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getProductoDescripcion() { return productoDescripcion; }
    public void setProductoDescripcion(String productoDescripcion) { this.productoDescripcion = productoDescripcion; }

    public String getProductoCodigo() { return productoCodigo; }
    public void setProductoCodigo(String productoCodigo) { this.productoCodigo = productoCodigo; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getGrupoNombre() { return grupoNombre; }
    public void setGrupoNombre(String grupoNombre) { this.grupoNombre = grupoNombre; }

    public String getMarcaNombre() { return marcaNombre; }
    public void setMarcaNombre(String marcaNombre) { this.marcaNombre = marcaNombre; }

    public boolean isDisponible() { return "DISPONIBLE".equals(estado); }
}
