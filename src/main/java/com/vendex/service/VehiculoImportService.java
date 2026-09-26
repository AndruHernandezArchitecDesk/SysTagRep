package com.vendex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendex.dao.VehiculoDAO;
import com.vendex.dao.VehiculoDAOPostgres;
import com.vendex.model.Vehiculo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Importa catálogo vehículos desde API pública NHTSA vPIC (https://vpic.nhtsa.dot.gov/api/).
 * No requiere key. Usado para poblar vehiculo sin tipeo manual.
 * Endpoints:
 * - GetMakesForVehicleType/car?format=json
 * - GetModelsForMake/{make}?format=json
 * - GetModelsForMakeYear/make/{make}/modelyear/{year}?format=json (opcional)
 */
public class VehiculoImportService {

    private static final Logger LOG = Logger.getLogger(VehiculoImportService.class.getName());
    private static final String BASE = "https://vpic.nhtsa.dot.gov/api/vehicles";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final VehiculoDAO dao;

    public VehiculoImportService() { this.dao = new VehiculoDAOPostgres(); }
    public VehiculoImportService(VehiculoDAO dao) { this.dao = dao; }

    public int importarMakes(int limitMakes, boolean soloComunes) {
        try {
            String url = BASE + "/GetMakesForVehicleType/car?format=json";
            JsonNode root = getJson(url);
            JsonNode results = root.path("Results");
            int importados = 0;
            for (JsonNode n : results) {
                if (importados >= limitMakes) break;
                String make = n.path("MakeName").asText();
                if (make == null || make.isBlank()) continue;
                if (soloComunes && !esMarcaComun(make)) continue;
                // Para cada make, importar 2-3 modelos de ejemplo
                int modelos = importarModelosParaMake(make, 3);
                importados++;
                LOG.info("Import make " + make + " -> " + modelos + " modelos");
                Thread.sleep(400); // throttling NHTSA
            }
            return importados;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "VehiculoImportService.importarMakes", e);
            return 0;
        }
    }

    public int importarModelosParaMake(String make, int limitModelos) {
        try {
            String url = BASE + "/GetModelsForMake/" + encode(make) + "?format=json";
            JsonNode root = getJson(url);
            JsonNode results = root.path("Results");
            int count = 0;
            for (JsonNode n : results) {
                if (count >= limitModelos) break;
                String model = n.path("Model_Name").asText();
                if (model == null || model.isBlank()) continue;
                Vehiculo v = new Vehiculo(make.toUpperCase(), model.toUpperCase(), null, null, null);
                v.setTipoVehiculo("Sedán");
                // Evitar duplicados por marca/modelo
                if (dao.buscar(make, model, null).isEmpty()) {
                    dao.guardar(v);
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "importarModelosParaMake " + make, e);
            return 0;
        }
    }

    public int importarVin(String vin17) {
        if (vin17 == null || vin17.length() != 17) return 0;
        try {
            String url = "https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVin/" + vin17 + "?format=json";
            JsonNode root = getJson(url);
            JsonNode results = root.path("Results");
            String make = null, model = null, yearStr = null;
            for (JsonNode r : results) {
                String var = r.path("Variable").asText();
                String val = r.path("Value").asText();
                if ("Make".equals(var)) make = val;
                if ("Model".equals(var)) model = val;
                if ("Model Year".equals(var)) yearStr = val;
            }
            if (make == null || model == null || make.isBlank() || model.isBlank()) return 0;
            Integer year = null;
            try { if (yearStr != null && !yearStr.isBlank()) year = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignore) {}
            Vehiculo v = new Vehiculo(make.toUpperCase(), model.toUpperCase(), year, year, null);
            if (dao.buscar(make, model, year).isEmpty()) {
                dao.guardar(v);
                return 1;
            }
            return 0;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "importarVin " + vin17, e);
            return 0;
        }
    }

    private boolean esMarcaComun(String make) {
        String m = make.toUpperCase();
        return m.contains("TOYOTA") || m.contains("CHEV") || m.contains("FORD") || m.contains("NISSAN") || m.contains("HYUNDAI") || m.contains("KIA") || m.contains("HONDA") || m.contains("SUZUKI") || m.contains("MAZDA") || m.contains("VOLKSWAGEN");
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().header("Accept", "application/json").build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IllegalStateException("NHTSA HTTP " + res.statusCode() + " url=" + url);
        return mapper.readTree(res.body());
    }

    private String encode(String s) { return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
}
