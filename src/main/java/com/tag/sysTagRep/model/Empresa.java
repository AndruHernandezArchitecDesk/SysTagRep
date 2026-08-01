package com.tag.sysTagRep.model;

public class Empresa {
    private int id;
    private String ruc;
    private String razonSocial;
    private String sucursal;
    private String direccionCallePrincipal;
    private String direccionCalleSecundaria;
    private String telefono;
    private String celular;
    private String correo;
    private String logoUrl;
    private String agenteRetencion;
    private String resolucion;
    private boolean estado;

    public Empresa(){}

    public Empresa(String ruc, String razon_social, String sucursal, String direccion_calle_principal, String direccion_calle_secundaria, String telefono, String celular, String correo, String logo_url){
        this.ruc = ruc;
        this.razonSocial = razon_social;
        this.sucursal = sucursal;
        this.direccionCallePrincipal = direccion_calle_principal;
        this.direccionCalleSecundaria = direccion_calle_secundaria;
        this.telefono = telefono;
        this.celular = celular;
        this.correo = correo;
        this.logoUrl = logo_url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getSucursal() {
        return sucursal;
    }

    public void setSucursal(String sucursal) {
        this.sucursal = sucursal;
    }

    public String getDireccionCallePrincipal() {
        return direccionCallePrincipal;
    }

    public void setDireccionCallePrincipal(String direccionCallePrincipal) {
        this.direccionCallePrincipal = direccionCallePrincipal;
    }

    public String getDireccionCalleSecundaria() {
        return direccionCalleSecundaria;
    }

    public void setDireccionCalleSecundaria(String direccionCalleSecundaria) {
        this.direccionCalleSecundaria = direccionCalleSecundaria;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getAgenteRetencion() {
        return agenteRetencion;
    }

    public void setAgenteRetencion(String agenteRetencion) {
        this.agenteRetencion = agenteRetencion;
    }

    public String getResolucion() {
        return resolucion;
    }

    public void setResolucion(String resolucion) {
        this.resolucion = resolucion;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

}


