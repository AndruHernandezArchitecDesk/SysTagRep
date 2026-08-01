package com.tag.sysTagRep.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class TerminosController {

    @FXML private Button btnCerrar;

    @FXML
    private void cerrar() {
        ((Stage) btnCerrar.getScene().getWindow()).close();
    }
}
