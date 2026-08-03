package com.tag.sysTagRep.util;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * Habilita en un ComboBox la escritura con filtrado a medida que se escribe.
 * El combo queda editable: al teclear se filtra el desplegable con los
 * elementos que contienen el texto y se autoselecciona si coincide exactamente.
 */
public class ComboFilter {

    private ComboFilter() {}

    public static <T> FilteredList<T> habilitar(ComboBox<T> combo, ObservableList<T> items, StringConverter<T> converter) {
        combo.setEditable(true);
        FilteredList<T> filtrados = new FilteredList<>(items, p -> true);
        combo.setItems(filtrados);
        StringConverter<T> conv = new StringConverter<>() {
            @Override public String toString(T t) { return texto(converter, t); }
            @Override public T fromString(String s) {
                if (s == null || s.trim().isEmpty()) return null;
                for (T item : items) {
                    String t = texto(converter, item);
                    if (t != null && t.equalsIgnoreCase(s.trim())) return item;
                }
                return converter.fromString(s);
            }
        };
        combo.setConverter(conv);

        combo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            String texto = newVal == null ? "" : newVal.trim();
            T seleccionado = combo.getSelectionModel().getSelectedItem();
            if (seleccionado == null || !texto(converter, seleccionado).equalsIgnoreCase(texto)) {
                String filtro = texto.toLowerCase();
                filtrados.setPredicate(item -> {
                    if (texto.isEmpty()) return true;
                    String t = texto(converter, item);
                    return t != null && t.toLowerCase().contains(filtro);
                });
                if (combo.isShowing() || combo.getEditor().isFocused()) {
                    combo.show();
                }
                for (T item : items) {
                    String t = texto(converter, item);
                    if (t != null && t.equalsIgnoreCase(texto)) {
                        combo.getSelectionModel().select(item);
                        break;
                    }
                }
            }
        });
        return filtrados;
    }

    public static void habilitar(ComboBox<String> combo, ObservableList<String> items) {
        habilitar(combo, items, new StringConverter<>() {
            @Override public String toString(String s) { return s == null ? "" : s; }
            @Override public String fromString(String s) { return s; }
        });
    }

    public static void habilitarEnteros(ComboBox<Integer> combo, ObservableList<Integer> items) {
        habilitar(combo, items, new StringConverter<>() {
            @Override public String toString(Integer i) { return i == null ? "" : String.valueOf(i); }
            @Override public Integer fromString(String s) {
                try { return s == null || s.isEmpty() ? null : Integer.parseInt(s); }
                catch (NumberFormatException e) { return null; }
            }
        });
    }

    private static <T> String texto(StringConverter<T> converter, T item) {
        if (converter == null) return item == null ? "" : String.valueOf(item);
        return converter.toString(item);
    }
}
