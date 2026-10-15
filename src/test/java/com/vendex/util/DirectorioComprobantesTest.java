package com.vendex.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DirectorioComprobantesTest {

    private String homeOriginal;

    @AfterEach
    void restaurarHome() {
        if (homeOriginal != null) System.setProperty("user.home", homeOriginal);
    }

    private Path homeTemporal() throws Exception {
        homeOriginal = System.getProperty("user.home");
        Path tmp = Files.createTempDirectory("vendex_home_test");
        System.setProperty("user.home", tmp.toString());
        return tmp;
    }

    @Test
    void sanitizar_limpiaCaracteresPeligrosos() {
        assertEquals("TAG Repuestos Automotrices",
                DirectorioComprobantes.sanitizar("TAG Repuestos Automotrices"));
        assertEquals("Mi Empresa S.A.", DirectorioComprobantes.sanitizar("Mi Empresa S.A./\\:*?\"<>|"));
        assertEquals("VENDEX", DirectorioComprobantes.sanitizar(null));
        assertEquals("VENDEX", DirectorioComprobantes.sanitizar("   "));
        assertEquals("VENDEX", DirectorioComprobantes.sanitizar("///"));
    }

    @Test
    void dirDia_creaEmpresaYFecha() throws Exception {
        Path home = homeTemporal();
        Path dir = DirectorioComprobantes.dirDia(LocalDate.of(2026, 10, 14));
        assertTrue(Files.isDirectory(dir), "Debe crear la carpeta del dia");
        assertTrue(dir.getFileName().toString().equals("2026-10-14"), "Formato ISO yyyy-MM-dd");
        assertTrue(dir.startsWith(home), "Dentro del home del usuario");
        assertEquals(3, dir.getNameCount() - home.getNameCount(),
                "Documentos/<Empresa>/yyyy-MM-dd = 3 niveles (Documentos puede variar por SO)");
    }

    @Test
    void dirDia_sinFecha_usaHoy() throws Exception {
        homeTemporal();
        Path dir = DirectorioComprobantes.dirDia();
        assertTrue(Files.isDirectory(dir));
        assertTrue(dir.getFileName().toString().equals(LocalDate.now().toString()));
    }
}
