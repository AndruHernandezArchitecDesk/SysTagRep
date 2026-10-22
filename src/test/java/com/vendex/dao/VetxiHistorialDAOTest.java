package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.VetxiHistorial.*;
import com.vendex.service.VetxiHistorialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VetxiHistorialDAOTest {
    private final Ambito ana = new Ambito(10, 1), juan = new Ambito(20, 1), otraSucursal = new Ambito(10, 2);
    private final VetxiHistorialDAO dao = new VetxiHistorialDAOPostgres();

    @BeforeEach void iniciar() {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:vetxi_" + UUID.randomUUID() + ";sql.syntax_pgs=true", "sa", "");
        DatabaseConnection.ensureVetxiHistorialSchema();
    }
    @AfterEach void cerrar() { DatabaseConnection.resetToDefault(); }

    private Mensaje mensaje(String conversacion, String texto, boolean usuario) {
        return new Mensaje(UUID.randomUUID().toString(), conversacion, "Consulta de repuestos", usuario, texto,
                false, usuario ? null : "VENTAS", List.of(), List.of(), null);
    }

    @Test void persisteOrdenMetadatosYFuentes_ySeRecuperaDesdeOtraInstancia() throws Exception {
        String id = UUID.randomUUID().toString();
        dao.guardar(ana, mensaje(id, "busco un filtro", true));
        var respuesta = new Mensaje(UUID.randomUUID().toString(), id, "Consulta de repuestos", false,
                "No disponible", true, null, List.of("FINANCIERO"),
                List.of(new Fuente("Catálogo", "https://example.com/filtro")), null);
        dao.guardar(ana, respuesta);
        var reiniciado = new VetxiHistorialDAOPostgres();
        var mensajes = reiniciado.mensajes(ana, id, 100);
        assertEquals(List.of("busco un filtro", "No disponible"), mensajes.stream().map(Mensaje::texto).toList());
        assertTrue(mensajes.get(1).sinIA());
        assertEquals(List.of("FINANCIERO"), mensajes.get(1).rolesBloqueados());
        assertEquals(respuesta.fuentes(), mensajes.get(1).fuentes());
        assertNotNull(mensajes.get(0).fecha());
        assertEquals(id, reiniciado.conversaciones(ana, 20).get(0).id());
    }

    @Test void mismoIdDeConversacion_noMezclaUsuariosNiSucursales() throws Exception {
        String id = UUID.randomUUID().toString();
        dao.guardar(ana, mensaje(id, "Ana sucursal 1", true));
        assertTrue(dao.conversaciones(juan, 20).isEmpty());
        assertTrue(dao.mensajes(juan, id, 100).isEmpty());
        assertTrue(dao.mensajes(otraSucursal, id, 100).isEmpty());
        dao.guardar(juan, mensaje(id, "Juan", true));
        dao.guardar(otraSucursal, mensaje(id, "Ana sucursal 2", true));
        assertEquals("Ana sucursal 1", dao.mensajes(ana, id, 100).get(0).texto());
        assertEquals("Juan", dao.mensajes(juan, id, 100).get(0).texto());
        assertEquals("Ana sucursal 2", dao.mensajes(otraSucursal, id, 100).get(0).texto());
    }

    @Test void reintentoDeMensaje_noDuplicaNiSobrescribe() throws Exception {
        String id = UUID.randomUUID().toString();
        var original = mensaje(id, "original", true);
        dao.guardar(ana, original); dao.guardar(ana, original);
        dao.guardar(ana, new Mensaje(original.id(), id, original.titulo(), true, "texto distinto", false,
                null, List.of(), List.of(), null));
        assertEquals(1, dao.mensajes(ana, id, 100).size());
        assertEquals("original", dao.mensajes(ana, id, 100).get(0).texto());
    }

    @Test void limitesRecuperanMensajesRecientesEnOrden_yConversacionesPorActividad() throws Exception {
        String primera = UUID.randomUUID().toString(), segunda = UUID.randomUUID().toString();
        dao.guardar(ana, mensaje(primera, "uno", true));
        dao.guardar(ana, mensaje(segunda, "dos", true));
        dao.guardar(ana, mensaje(primera, "tres", false));
        assertEquals(primera, dao.conversaciones(ana, 1).get(0).id());
        assertEquals(List.of("tres"), dao.mensajes(ana, primera, 1).stream().map(Mensaje::texto).toList());
        assertEquals(List.of("uno", "tres"), dao.mensajes(ana, primera, 100).stream().map(Mensaje::texto).toList());
    }

    @Test void servicioRechazaSesionAusenteOCambiada_antesDeLeerOEscribir() throws Exception {
        AtomicReference<Ambito> actual = new AtomicReference<>(ana);
        var servicio = new VetxiHistorialService(dao, actual::get);
        String id = UUID.randomUUID().toString();
        servicio.guardar(ana, mensaje(id, "privado", true));
        actual.set(juan);
        assertThrows(SecurityException.class, () -> servicio.mensajes(ana, id));
        assertThrows(SecurityException.class, () -> servicio.guardar(ana, mensaje(id, "tardío", false)));
        assertTrue(servicio.conversaciones(juan).isEmpty());
        actual.set(otraSucursal);
        assertThrows(SecurityException.class, () -> servicio.conversaciones(ana));
        actual.set(null);
        assertThrows(SecurityException.class, () -> servicio.conversaciones(ana));
        assertThrows(SecurityException.class, () -> servicio.conversaciones(null));
        assertEquals(1, dao.mensajes(ana, id, 100).size());
    }
}
