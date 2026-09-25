package com.vendex.backup;

import java.io.File;

public class BackupResult {
    public final boolean exito;
    public final String mensaje;
    public final File archivoLocal; // .dump.gpg o .dump si sin cifrado
    public final File archivoDumpOriginal; // .dump antes de cifrar (si se conservó)
    public final long tamanioBytes;
    public final long duracionMs;
    public final boolean copiadoOffsite;
    public final String offsiteDestino;
    public final String errorOffsite;

    public BackupResult(boolean exito, String mensaje, File archivoLocal, File archivoDumpOriginal,
                        long tamanioBytes, long duracionMs,
                        boolean copiadoOffsite, String offsiteDestino, String errorOffsite) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.archivoLocal = archivoLocal;
        this.archivoDumpOriginal = archivoDumpOriginal;
        this.tamanioBytes = tamanioBytes;
        this.duracionMs = duracionMs;
        this.copiadoOffsite = copiadoOffsite;
        this.offsiteDestino = offsiteDestino;
        this.errorOffsite = errorOffsite;
    }

    public static BackupResult falla(String mensaje) {
        return new BackupResult(false, mensaje, null, null, 0, 0, false, null, null);
    }
}
