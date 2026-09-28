package com.vendex.remote;

import com.vendex.dao.UsuarioDAO;
import com.vendex.model.Usuario;
import java.util.List;
import java.lang.String;

public class UsuarioDAORest extends RestDao<Usuario> implements UsuarioDAO {
    public UsuarioDAORest(RestClient rest) { super(rest, "/usuarios", Usuario.class); }
    public Usuario autenticar(String username, String password) { throw new UnsupportedOperationException("Usuario.autenticar rest"); }
    public Usuario buscarPorUsername(String username) { throw new UnsupportedOperationException("Usuario.buscarPorUsername rest"); }
    public boolean verificarPassword(Usuario u, String passwordPlano) { throw new UnsupportedOperationException("Usuario.verificarPassword rest"); }
    public void incrementarIntentoFallido(int id) { throw new UnsupportedOperationException("Usuario.incrementarIntentoFallido rest"); }
    public void bloquear(int id, java.time.LocalDateTime hasta) { throw new UnsupportedOperationException("Usuario.bloquear rest"); }
    public void resetearIntentos(int id) { throw new UnsupportedOperationException("Usuario.resetearIntentos rest"); }
    public void resetearSiVentanaExpirada(int id, java.time.LocalDateTime ultimoIntento, int ventanaMinutos) { throw new UnsupportedOperationException("Usuario.resetearSiVentanaExpirada rest"); }
    public void desbloquear(int id) { throw new UnsupportedOperationException("Usuario.desbloquear rest"); }
    public List<Usuario> listar() { throw new UnsupportedOperationException("Usuario.listar rest"); }
    public Usuario obtenerPorId(int id) { throw new UnsupportedOperationException("Usuario.obtenerPorId rest"); }
    public int guardar(Usuario u) { throw new UnsupportedOperationException("Usuario.guardar rest"); }
    public void actualizar(Usuario u) { throw new UnsupportedOperationException("Usuario.actualizar rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Usuario.eliminar rest"); }
}