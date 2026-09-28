package com.vendex.remote;

import com.vendex.dao.ConfiguracionEmailDAO;
import com.vendex.model.ConfiguracionEmail;
import java.util.Optional;
import java.lang.String;

public class ConfiguracionEmailDAORest extends RestDao<ConfiguracionEmail> implements ConfiguracionEmailDAO {
    public ConfiguracionEmailDAORest(RestClient rest) { super(rest, "/configuracion-email", ConfiguracionEmail.class); }
    public Optional<ConfiguracionEmail> obtenerActiva() { throw new UnsupportedOperationException("ConfiguracionEmail.obtenerActiva rest"); }
    public void guardar(ConfiguracionEmail cfg) { throw new UnsupportedOperationException("ConfiguracionEmail.guardar rest"); }
    public String obtenerPasswordPlano(ConfiguracionEmail cfg) { throw new UnsupportedOperationException("ConfiguracionEmail.obtenerPasswordPlano rest"); }
    public String probarConexion(ConfiguracionEmail cfg, String passwordPlano) { throw new UnsupportedOperationException("ConfiguracionEmail.probarConexion rest"); }
    public String probarEnvio(ConfiguracionEmail cfg, String passwordPlano, String destinatario) { throw new UnsupportedOperationException("ConfiguracionEmail.probarEnvio rest"); }
}