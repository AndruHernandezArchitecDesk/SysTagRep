package com.tag.sysTagRep.util;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class SortTable {

    private SortTable() {}

    public static void agregarBotones(TableView<?> tabla) {
        for (TableColumn<?, ?> col : tabla.getColumns()) {
            configurarColumna(tabla, col);
        }
    }

    private static void configurarColumna(TableView<?> tabla, TableColumn<?, ?> col) {
        if (!col.getColumns().isEmpty()) {
            for (TableColumn<?, ?> sub : col.getColumns()) {
                configurarColumna(tabla, sub);
            }
            return;
        }
        col.setSortable(true);

        Button btn = new Button("⇅");
        btn.getStyleClass().add("btn-orden");
        btn.setFocusTraversable(false);
        btn.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);

        btn.setOnAction(e -> {
            SortType tipo = col.getSortType() == SortType.ASCENDING
                    ? SortType.DESCENDING : SortType.ASCENDING;
            col.setSortType(tipo);
            @SuppressWarnings("unchecked")
            TableView<Object> raw = (TableView<Object>) tabla;
            if (!raw.getSortOrder().contains(col)) {
                raw.getSortOrder().clear();
                raw.getSortOrder().add((TableColumn<Object, ?>) col);
            } else {
                raw.sort();
            }
            actualizarIcono(btn, tipo);
        });

        col.sortTypeProperty().addListener((obs, viejo, nuevo) ->
                actualizarIcono(btn, nuevo));
        actualizarIcono(btn, col.getSortType());

        Label lbl = new Label(col.getText());
        HBox box = new HBox(4, lbl, btn);
        box.setAlignment(Pos.CENTER);
        col.setText("");
        col.setGraphic(box);
    }

    private static void actualizarIcono(Button btn, SortType tipo) {
        btn.setText(tipo == SortType.DESCENDING ? "↓" : "↑");
    }
}
