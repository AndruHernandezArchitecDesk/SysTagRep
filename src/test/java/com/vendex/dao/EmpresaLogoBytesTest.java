package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Empresa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmpresaLogoBytesTest {
    private final EmpresaDAOPostgres dao = new EmpresaDAOPostgres();

    @BeforeEach
    void preparar() throws Exception {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:empresa_logo_" + java.util.UUID.randomUUID()
                + ";sql.syntax_pgs=true", "sa", "");
        try (var con = DatabaseConnection.getConnection(); var st = con.createStatement()) {
            st.execute("CREATE TABLE empresa (id INTEGER PRIMARY KEY, ruc VARCHAR(13), razon_social VARCHAR(300), "
                    + "titulo VARCHAR(300), sucursal VARCHAR(150), direccion_calle_principal VARCHAR(150), "
                    + "direccion_calle_secundaria VARCHAR(150), telefono VARCHAR(20), celular VARCHAR(20), "
                    + "correo VARCHAR(100), logo_url VARCHAR(500), logo_bytes VARBINARY(5242880), "
                    + "agente_retencion VARCHAR(20), resolucion VARCHAR(50), obligado_contabilidad VARCHAR(2), estado BOOLEAN)");
            st.execute("INSERT INTO empresa(id,ruc,razon_social,logo_url,logo_bytes,obligado_contabilidad,estado) "
                    + "VALUES (1,'1790000000001','TAG Repuestos','/legacy/logo.png',NULL,'NO',TRUE)");
        }
    }

    @AfterEach
    void cerrar() { DatabaseConnection.shutdown(); }

    @Test
    void actualizaYLeeLogoCompartidoSinBorrarCuandoUpdateNoIncluyeLogo() {
        byte[] logo = {0, 1, 2, 3, 4, 5};
        Empresa empresa = dao.obtenerPorId(1);
        assertNull(empresa.getLogoBytes());
        empresa.setLogoBytes(logo);
        assertTrue(dao.actualizar(empresa));
        assertArrayEquals(logo, dao.obtenerPorId(1).getLogoBytes());

        Empresa edicionDeDatos = dao.obtenerPorId(1);
        edicionDeDatos.setRazonSocial("TAG Repuestos Automotrices");
        edicionDeDatos.setLogoBytes(null);
        assertTrue(dao.actualizar(edicionDeDatos));
        assertArrayEquals(logo, dao.listar().get(0).getLogoBytes());
    }

    @Test
    void modeloCopiaLosBytesParaEvitarCambiosExternos() {
        byte[] original = {10, 20, 30};
        Empresa empresa = new Empresa();
        empresa.setLogoBytes(original);
        original[0] = 99;
        byte[] leido = empresa.getLogoBytes();
        assertArrayEquals(new byte[]{10, 20, 30}, leido);
        leido[1] = 99;
        assertArrayEquals(new byte[]{10, 20, 30}, empresa.getLogoBytes());
    }
}
