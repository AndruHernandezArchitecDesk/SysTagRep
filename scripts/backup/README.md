# Backup Vendex — Windows (cierre de caja)

Estrategia lineamiento `LINEAMIENTO_BACKUP_POSTGRESQL.md` adaptada a Windows + otra PC en red.

## Qué hace
- `pg_dump --format=custom --compress=9` diario (al cierre de caja y manual).
- Verificación no vacío, cifrado `gpg --symmetric AES256` con passphrase en `SecureConfigStore` Tier1 (`~/.vendex/secrets/backup.passphrase.enc`).
- Copia offsite automática a `\\OTRA-PC\VendexBackups` (configurable).
- Retención GFS: 7 diarios, 4 semanales, 12 mensuales (carpeta local; offsite espejo).
- Notificación a `andresrockfull@gmail.com` vía `ConfiguracionEmail` si falla.

## Pre-requisitos en 192.168.1.7 (host BD)
1. PostgreSQL con `pg_dump` en PATH (`C:\Program Files\PostgreSQL\17\bin\pg_dump.exe`)
   ```bat
   pg_dump --version
   ```
   Si no está en PATH, configurar en `~/.vendex/backup.properties`:
   ```
   backup.pg_dump.path=C:\\Program Files\\PostgreSQL\\17\\bin\\pg_dump.exe
   ```

2. Gpg4win (`gpg --version`) — https://www.gpg4win.org/
   ```
   backup.gpg.path=C:\\Program Files (x86)\\GnuPG\\bin\\gpg.exe
   ```
   Si no hay gpg, el backup se guarda sin cifrar (warning).

3. Carpeta compartida en otra PC de la red:
   - En otra PC: crear `C:\VendexBackups` → Compartir → `\\OTRA-PC\VendexBackups` con permisos escritura para usuario de 192.168.1.7.
   - En 192.168.1.7 probar:
     ```bat
     dir \\OTRA-PC\VendexBackups
     echo test > \\OTRA-PC\VendexBackups\test.txt
     ```
   - Configurar en Vendex: Administración → Respaldos → Ruta offsite.

## Uso
- **Automático al cierre de caja:** al cerrar caja (Caja → Cerrar Caja) se dispara backup en segundo plano.
- **Manual:** Administración → Respaldos → “Respaldar ahora”.

## Scripts
- `backup_vendex.bat` — backup manual desde Task Scheduler (opcional, además del cierre de caja).
  ```bat
  scripts\backup\backup_vendex.bat
  ```
  Programar con Task Scheduler (opcional):
  ```bat
  schtasks /create /tn "VendexBackup" /tr "\"C:\Vendex\scripts\backup\backup_vendex.bat\"" /sc daily /st 19:00 /ru SYSTEM
  ```

## Verificación
```bat
dir C:\Vendex\backups
gpg --decrypt C:\Vendex\backups\vendex_*.dump.gpg > NUL && echo OK
pg_restore --list C:\Vendex\backups\vendex_*.dump | head
dir \\OTRA-PC\VendexBackups
```

## Restauración rápida
```bat
REM desencriptar
gpg --decrypt C:\Vendex\backups\vendex_20260922_190000.dump.gpg > C:\temp\restore.dump
REM restaurar
pg_restore -h localhost -U postgres -d dbVendex --clean --if-exists C:\temp\restore.dump
REM o tabla individual
pg_restore -h localhost -U postgres -d dbVendex -t factura_registro C:\temp\restore.dump
```

## Retención
Automática GFS en Java (`BackupRetentionService`). Si necesita limpieza manual:
```bat
forfiles /p C:\Vendex\backups /m vendex_*.dump.gpg /d -7 /c "cmd /c del @path"
```
Pero preferir que lo haga Vendex.
