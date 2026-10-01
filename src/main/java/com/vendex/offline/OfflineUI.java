package com.vendex.offline;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public final class OfflineUI {

    private OfflineUI() {}

    public static void mostrarAlertaEncolada(String operacion) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Modo offline");
        alert.setHeaderText("Operación encolada");
        alert.setContentText("Modo OFFLINE\n\n" + operacion + " encolada para sincronizar cuando haya conexión.\n\nLa operación se guardará localmente y se enviará al backend al reconectar.");
        alert.showAndWait();
    }

    public static void mostrarAlertaEncoladaMensaje(String mensaje) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Modo offline");
        alert.setHeaderText("Operación encolada");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
