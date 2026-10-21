package com.vendex.controller.api;

import com.vendex.backup.BackupConfig;
import com.vendex.backup.BackupResult;
import com.vendex.backup.BackupService;
import io.javalin.http.Context;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fase 6b: ejecución y listado de respaldos vía API (permiso USUARIO_GESTIONAR). */
public class BackupApiController {

    private final BackupService servicio = new BackupService();

    public void ejecutar(Context ctx) {
        BackupResult r = servicio.ejecutar();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exito", r.exito);
        out.put("mensaje", r.mensaje);
        out.put("archivo", r.archivoLocal != null ? r.archivoLocal.getAbsolutePath() : null);
        out.put("tamanioBytes", r.tamanioBytes);
        out.put("duracionMs", r.duracionMs);
        out.put("copiadoOffsite", r.copiadoOffsite);
        out.put("offsiteDestino", r.offsiteDestino);
        if (r.errorOffsite != null) out.put("errorOffsite", r.errorOffsite);
        ctx.status(r.exito ? 200 : 502).json(out);
    }

    public void listar(Context ctx) {
        File dir = new File(BackupConfig.getBackupDir());
        File[] archivos = dir.listFiles((d, name) -> name.endsWith(".dump") || name.endsWith(".dump.gpg"));
        ctx.json(Map.of(
                "directorio", dir.getAbsolutePath(),
                "existe", dir.isDirectory(),
                "archivos", archivos == null ? java.util.List.of()
                        : Arrays.stream(archivos)
                            .sorted((a, b) -> Long.compare(b.lastModified(), a.lastModified()))
                            .map(f -> Map.of(
                                    "nombre", f.getName(),
                                    "tamanioBytes", f.length(),
                                    "modificado", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date(f.lastModified()))))
                            .toList()
        ));
    }
}
