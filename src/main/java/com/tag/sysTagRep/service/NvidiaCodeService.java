package com.tag.sysTagRep.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NvidiaCodeService {

    private final String apiKey = "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";
    private final String baseUrl = "https://api.nvidia.com/v1/chat/completions";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Genera código Java basado en una descripción en natural
     *
     * @param prompt Descripción de qué código generar
     * @param modelo Nombre del modelo (ej: "nemotron", "code-llama")
     * @return Código Java generado por la IA
     */
    public String generarCodigo(String prompt, String modelo) {
        String jsonBody = String.format(
                "{\"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
                modelo, prompt.replace("\"", "\\\"")
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Error " + response.statusCode() + ": " + response.body());
            }

            // Parsear respuesta JSON para extraer el contenido
            return extraerCodigoDeRespuesta(response.body());
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con la API NVIDIA: " + e.getMessage());
        }
    }

/**
     * Genera código Java con modelo por defecto (nemotron)
     */
    public String generarCodigo(String prompt) {
        return generarCodigo(prompt, "nemotron");
    }

    /**
     * Extrae el contenido de la respuesta de la API NVIDIA
     */
    private String extraerCodigoDeRespuesta(String jsonResponse) {
        // JSON simple sin dependencias externas
        // Formato esperado: {"choices":[{"message":{"content":"..."}}]}
        try {
            // Buscar el contenido entre messages
            String contenido = jsonResponse;
            int startIdx = jsonResponse.indexOf("\"content\"");
            if (startIdx > 0) {
                int valorStart = jsonResponse.indexOf(":", startIdx) + 1;
                int valorEnd = jsonResponse.lastIndexOf("}", valorStart);
                if (valorEnd > valorStart) {
                    String valor = jsonResponse.substring(valorStart, valorEnd).trim();
                    // Remover comillas si están
                    if (valor.startsWith("\"") && valor.endsWith("\"")) {
                        valor = valor.substring(1, valor.length() - 1);
                    }
                    return valor;
                }
            }
        } catch (Exception e) {
            // En caso de error en parsing, devolver la respuesta completa
        }
        return jsonResponse;
    }

    /**
     * Caso de uso: Generar método para validar RUC
     */
    public String generarValidadorRuc() {
        return generarCodigo(
                "Genera un método Java validadRuc(String ruc) que retorne boolean y valide que el RUC tenga exactamente 11 dígitos numéricos. Incluye JavaDoc."
        );
    }

    /**
     * Caso de uso: Generar controlador FXML
     */
    public String generarControladorEmpresa() {
        return generarCodigo(
                "Genera un controlador JavaFX FXML para la gestión de empresa con campos: razonSocial, titulo, telefono, correo, direccion, sucursal y logo. Incluye botones guardar y cancelar."
        );
    }

    /**
     * Caso de uso: Generar validador de email
     */
    public String generarValidadorEmail() {
        return generarCodigo(
                "Genera un método Java validarEmail(String email) que retorne boolean y valide el formato de email usando expresión regular. Incluye JavaDoc."
        );
    }
}