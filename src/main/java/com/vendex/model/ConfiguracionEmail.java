package com.vendex.model;

import java.time.LocalDateTime;

public class ConfiguracionEmail {
    private int id;
    private String hostSmtp;
    private int puertoSmtp;
    private boolean usarTls;
    private String emailRemitente;
    private String nombreRemitente;
    private String usuarioSmtp;
    private String passwordCifrado;
    private String replyTo;
    private boolean activo;
    private LocalDateTime actualizadoEn;
    private Integer actualizadoPor;

    public ConfiguracionEmail() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getHostSmtp() { return hostSmtp; }
    public void setHostSmtp(String hostSmtp) { this.hostSmtp = hostSmtp; }

    public int getPuertoSmtp() { return puertoSmtp; }
    public void setPuertoSmtp(int puertoSmtp) { this.puertoSmtp = puertoSmtp; }

    public boolean isUsarTls() { return usarTls; }
    public void setUsarTls(boolean usarTls) { this.usarTls = usarTls; }

    public String getEmailRemitente() { return emailRemitente; }
    public void setEmailRemitente(String emailRemitente) { this.emailRemitente = emailRemitente; }

    public String getNombreRemitente() { return nombreRemitente; }
    public void setNombreRemitente(String nombreRemitente) { this.nombreRemitente = nombreRemitente; }

    public String getUsuarioSmtp() { return usuarioSmtp; }
    public void setUsuarioSmtp(String usuarioSmtp) { this.usuarioSmtp = usuarioSmtp; }

    public String getPasswordCifrado() { return passwordCifrado; }
    public void setPasswordCifrado(String passwordCifrado) { this.passwordCifrado = passwordCifrado; }

    public String getReplyTo() { return replyTo; }
    public void setReplyTo(String replyTo) { this.replyTo = replyTo; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    public Integer getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(Integer actualizadoPor) { this.actualizadoPor = actualizadoPor; }
}
