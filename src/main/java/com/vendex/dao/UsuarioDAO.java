package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Usuario;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface UsuarioDAO {

    Usuario autenticar(String username, String password);

    Usuario buscarPorUsername(String username);

    boolean verificarPassword(Usuario u, String passwordPlano);

    void incrementarIntentoFallido(int id);

    void bloquear(int id, java.time.LocalDateTime hasta);

    void resetearIntentos(int id);

    void resetearSiVentanaExpirada(int id, java.time.LocalDateTime ultimoIntento, int ventanaMinutos);

    void desbloquear(int id);

    List<Usuario> listar();

    Usuario obtenerPorId(int id);

    int guardar(Usuario u);

    void actualizar(Usuario u);

    void eliminar(int id);

}
