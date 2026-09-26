package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.ConfiguracionEmail;
import com.vendex.util.Cifrado;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Optional;
import java.util.Properties;

public interface ConfiguracionEmailDAO {

    Optional<ConfiguracionEmail> obtenerActiva();

    void guardar(ConfiguracionEmail cfg) throws SQLException;

    String obtenerPasswordPlano(ConfiguracionEmail cfg);

    String probarConexion(ConfiguracionEmail cfg, String passwordPlano);

    String probarEnvio(ConfiguracionEmail cfg, String passwordPlano, String destinatario);

}
