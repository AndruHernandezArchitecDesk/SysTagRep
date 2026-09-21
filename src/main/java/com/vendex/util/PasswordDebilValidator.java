package com.vendex.util;

import java.util.Set;

/**
 * Valida contraseñas de BD obviamente débiles.
 * Usado por {@link com.vendex.config.DatabaseConnection} al arrancar: solo warning+log, no bloquea.
 */
public final class PasswordDebilValidator {

    private static final Set<String> DEBILES = Set.of(
            "admin", "postgres", "password", "123456", "12345678",
            "vendex", "admin123", "postgres123", "12345", "root", "qwerty"
    );

    private PasswordDebilValidator() {}

    public static boolean esDebil(String password) {
        if (password == null || password.isBlank()) return true;
        String t = password.trim().toLowerCase();
        if (DEBILES.contains(t)) return true;
        if (t.length() < 8) return true;
        return false;
    }

    public static String mensajeAdvertencia() {
        return "La contraseña de base de datos configurada es insegura. "
                + "Contacta a soporte para regenerarla antes de continuar.\n\n"
                + "Esta advertencia se muestra porque la contraseña es débil o es un valor por defecto (ej. 'admin'). "
                + "Puedes continuar, pero se recomienda rotarla cuanto antes en todas las PCs de la instalación.";
    }
}
