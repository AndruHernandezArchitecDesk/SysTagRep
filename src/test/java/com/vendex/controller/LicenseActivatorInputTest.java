package com.vendex.controller;

import com.vendex.MainApp;
import com.vendex.util.LicenseManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LicenseActivatorInputTest {
    @BeforeAll
    static void iniciarJavaFx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) { }
        Platform.setImplicitExit(false);
    }

    @Test
    void licenciaCentralConservaFirmaAlPegarEnPantallaReal() throws Exception {
        FutureTask<Void> tarea = new FutureTask<>(() -> {
            String anterior = System.getProperty("VENDEX_LICENSE_HMAC_KEY");
            System.setProperty("VENDEX_LICENSE_HMAC_KEY", "prueba-entrada-licencia");
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/view/LicenseActivatorView.fxml"));
                var aplicar = MainApp.class.getDeclaredMethod("aplicarMayusculas", Node.class);
                aplicar.setAccessible(true);
                aplicar.invoke(new MainApp(), root);
                TextField campo = (TextField) root.lookup("#txtLicenseKey");
                String maquina = LicenseManager.getMachineCode();
                String clave = LicenseManager.generateLicenseKeyV2(maquina, LocalDate.now().plusDays(4),
                        LicenseManager.MODALIDAD_CENTRAL, false, List.of(), 0);
                assertNotNull(clave);
                assertNotEquals(clave.toUpperCase(java.util.Locale.ROOT), clave);
                campo.replaceText(0, campo.getLength(), clave);
                assertEquals(clave, campo.getText(), "La pantalla no debe convertir Base64 a mayúsculas");
                assertTrue(LicenseManager.validateLicenseKey(maquina, campo.getText()));
                campo.replaceText(0, campo.getLength(), clave.replace("-", ""));
                assertTrue(LicenseManager.validateLicenseKey(maquina, campo.getText()));
            } finally {
                if (anterior == null) System.clearProperty("VENDEX_LICENSE_HMAC_KEY");
                else System.setProperty("VENDEX_LICENSE_HMAC_KEY", anterior);
            }
            return null;
        });
        Platform.runLater(tarea);
        tarea.get(30, TimeUnit.SECONDS);
    }
}
