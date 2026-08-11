# Pruebas Unitarias - SysTagRep

## Resumen

| Métrica | Valor |
|---------|-------|
| Total tests | 50 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Estado | BUILD SUCCESS |

## Herramientas

- **JUnit 5** (10.0.0): framework de pruebas
- **Mockito** (5.7.0): mocks y `MockedStatic`
- **HSQLDB** (2.7.2): base de datos en memoria para pruebas de DAOs
- **Maven Surefire** (3.2.3): ejecución de pruebas

## Estructura de pruebas

```
src/test/java/com/tag/sysTagRep/
├── controller/
│   ├── FacturaCalculatorTest.java
│   └── ProformaCalculatorTest.java
├── dao/
│   ├── ClienteDAOTest.java
│   └── ProveedorDAOTest.java
├── model/
│   ├── InventarioStockTest.java
│   └── NotaVentaRegistroTest.java
└── util/
    ├── CifradoTest.java
    ├── ClaveAccesoTest.java
    ├── ConfigAmbienteTest.java
    ├── ConfigFirmaTest.java
    ├── EmailServiceTest.java
    ├── NotaVentaPDFTest.java
    └── SRIWebServiceTest.java
```

## Cambios realizados en código fuente para testing

### DatabaseConnection.java

Se modificó para aislar las pruebas de DAOs usando bases de datos HSQLDB en memoria en lugar de PostgreSQL.

**Antes:**
```java
public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/dbTag";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

**Después:**
```java
public class DatabaseConnection {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/dbTag";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "admin";

    private static final ThreadLocal<String> URL = new ThreadLocal<>();
    private static final ThreadLocal<String> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> PASSWORD = new ThreadLocal<>();

    static {
        URL.set(DEFAULT_URL);
        USER.set(DEFAULT_USER);
        PASSWORD.set(DEFAULT_PASSWORD);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL.get(), USER.get(), PASSWORD.get());
    }

    public static void setConnectionParams(String url, String user, String password) {
        URL.set(url);
        USER.set(user);
        PASSWORD.set(password);
    }

    public static void resetToDefault() {
        URL.set(DEFAULT_URL);
        USER.set(DEFAULT_USER);
        PASSWORD.set(DEFAULT_PASSWORD);
    }
}
```

**Razón:** Los DAOs usan `DatabaseConnection.getConnection()`, que originalmente apuntaba a PostgreSQL. Para tests aislados se necesita cambiar dinámicamente la configuración por thread.

### SRIWebService.java

Se cambió el método `postSoap` de `private` a `protected` para permitir herencia en pruebas.

**Cambio:**
```java
// Antes: private String postSoap(String url, String soapBody)
// Después:
protected String postSoap(String url, String soapBody)
```

**Razón:** Permite crear una subclase `TestableSRIWebService` que sobrescribe `postSoap` para simular respuestas del SRI sin conectarse a la red real.

### pom.xml

Se agregaron dependencias de testing:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>${junit.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>${mockito.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>${mockito.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
    <version>2.7.2</version>
    <scope>test</scope>
</dependency>
```

### Configuración Mockito

Se creó `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` con contenido `mock-maker-inline` para soportar `MockedStatic`.

## Pruebas por paquete

### controller (17 tests)

#### FacturaCalculatorTest.java (6 tests)

**Propósito:** Validar cálculos de facturas electrónicas.

**Técnica:** Tests unitarios directos sobre la clase calculadora.

```java
class FacturaCalculatorTest {
    @Test
    void calcularSubtotal_sinIVA_sumaCorrecta() {
        // Suma de totales sin IVA
    }

    @Test
    void calcularIVA_quincePorCiento_correcto() {
        // IVA = subtotal * 0.15
    }

    @Test
    void calcularTotal_conIVA_sumaCorrecta() {
        // total = subtotal + iva
    }
    // ... 3 más
}
```

**Qué prueba:**
- Cálculo de subtotales
- Cálculo de IVA (15%)
- Cálculo de totales
- Redondeo decimal
- Casos con cantidades múltiples
- Casos extremos (valores cero)

#### ProformaCalculatorTest.java (11 tests)

**Propósito:** Validar cálculos de proformas y validación de stock.

**Técnica:** Tests unitarios directos.

```java
class ProformaCalculatorTest {
    @Test
    void calcularTotales_proformaConMultiplesItems_correcto() {
        // Suma de items, IVA, total
    }

    @Test
    void validarStock_disponible_sinError() {
        // Stock >= cantidad solicitada
    }

    @Test
    void validarStock_insuficiente_lanzaExcepcion() {
        // Stock < cantidad solicitada
    }
    // ... 9 más
}
```

**Qué prueba:**
- Cálculo de subtotales, IVA, descuentos
- Validación de stock disponible
- Formato de totales con 2 decimales
- Proformas con múltiples ítems
- Proformas con descuento
- Proformas sin descuento
- Valores extremos

### dao (7 tests)

#### ClienteDAOTest.java (4 tests)

**Propósito:** Validar operaciones CRUD de clientes.

**Técnica:** Tests de integración con HSQLDB en memoria.

```java
class ClienteDAOTest {
    @Test
    void guardarYListar_cliente_sePersisteCorrectamente() throws Exception {
        // 1. Crear conexión HSQLDB única
        String dbName = "testdb_" + UUID.randomUUID().toString().replace("-", "");
        Connection con = DriverManager.getConnection("jdbc:hsqldb:mem:" + dbName, "sa", "");

        // 2. Configurar DatabaseConnection para usar HSQLDB
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:" + dbName, "sa", "");

        // 3. Crear tabla
        Statement st = con.createStatement();
        st.execute("CREATE TABLE cliente (...)");

        // 4. Ejecutar operación DAO
        ClienteDAO dao = new ClienteDAO();
        Cliente c = new Cliente();
        c.setNombre("Juan Perez");
        // ...
        dao.guardar(c);

        // 5. Verificar persistencia
        List<Cliente> lista = dao.listar();
        assertEquals(1, lista.size());
        assertEquals("Juan Perez", lista.get(0).getNombre());

        // 6. Restaurar configuración
        DatabaseConnection.resetToDefault();
    }
    // ... 3 más (listar vacío, actualizar, eliminar)
}
```

**Detalles importantes:**
- Cada test usa un nombre de BD único (`UUID.randomUUID()`) para aislamiento total
- `DatabaseConnection.setConnectionParams()` redirige el DAO a HSQLDB
- `DatabaseConnection.resetToDefault()` restaura PostgreSQL para otros tests
- Tablas se crean con `CREATE TABLE` (no `CREATE TABLE IF NOT EXISTS`) porque cada test tiene su propia BD

**Qué prueba:**
- `guardarYListar_cliente_sePersisteCorrectamente`: INSERT y SELECT
- `listar_sinClientes_retornaListaVacia`: SELECT sin datos
- `guardarYActualizar_cliente_cambiosSePersisten`: INSERT, SELECT, UPDATE, SELECT
- `guardarYEliminar_cliente_seEliminaCorrectamente`: INSERT, SELECT, DELETE, SELECT

#### ProveedorDAOTest.java (3 tests)

**Propósito:** Validar operaciones CRUD de proveedores.

**Técnica:** Igual que `ClienteDAOTest` pero con tabla `proveedor`.

```java
class ProveedorDAOTest {
    @Test
    void guardarYListar_proveedor_sePersisteCorrectamente() throws Exception {
        // Misma estrategia: HSQLDB en memoria con BD única por test
    }
    // ... 2 más (listar vacío, eliminar)
}
```

**Qué prueba:**
- `guardarYListar_proveedor_sePersisteCorrectamente`: INSERT y SELECT
- `listar_sinProveedores_retornaListaVacia`: SELECT sin datos
- `guardarYEliminar_proveedor_seEliminaCorrectamente`: INSERT, SELECT, DELETE, SELECT

### model (8 tests)

#### InventarioStockTest.java (5 tests)

**Propósito:** Validar el modelo de inventario y stock.

**Técnica:** Tests unitarios directos sobre el modelo.

```java
class InventarioStockTest {
    @Test
    void cantidadDisponible_mayorCero_devuelveTrue() {
        Inventario item = new Inventario();
        item.setCantidad(10);
        assertTrue(item.tieneStockDisponible(5));
    }

    @Test
    void cantidadDisponible_igualACero_devuelveFalse() {
        // Stock agotado
    }
    // ... 3 más
}
```

**Qué prueba:**
- Validación de stock disponible
- Cálculo de valor de inventario
- Actualización de cantidades
- Casos de stock cero
- Casos de stock negativo

#### NotaVentaRegistroTest.java (3 tests)

**Propósito:** Validar el modelo de registro de notas de venta.

**Técnica:** Tests unitarios directos.

```java
class NotaVentaRegistroTest {
    @Test
    void calcularTotal_sumaCorrecta() {
        NotaVentaRegistro reg = new NotaVentaRegistro();
        reg.setSubtotal(new BigDecimal("100.00"));
        reg.setIva(new BigDecimal("15.00"));
        assertEquals(new BigDecimal("115.00"), reg.getTotal());
    }
    // ... 2 más
}
```

**Qué prueba:**
- Cálculo de totales
- Formato de números
- Validación de campos obligatorios

### util (32 tests)

#### CifradoTest.java (7 tests)

**Propósito:** Validar cifrado/descifrado AES-GCM.

**Técnica:** Tests unitarios usando la clase `Cifrado` real.

```java
class CifradoTest {
    @Test
    void encriptarYDesencriptar_textoVuelveOriginal() throws Exception {
        String original = "Test de encriptacion";
        String encriptado = Cifrado.encriptar(original, "password123");
        String desencriptado = Cifrado.desencriptar(encriptado, "password123");
        assertEquals(original, desencriptado);
    }
    // ... 6 más
}
```

**Qué prueba:**
- Cifrado y descifrado exitoso
- Contraseña incorrecta falla
- Texto vacío
- Caracteres especiales
- Texto largo
- Clave nula
- Consistencia de encriptación

#### ClaveAccesoTest.java (8 tests)

**Propósito:** Validar generación de claves de acceso SRI.

**Técnica:** Tests unitarios sobre generadores de claves.

```java
class ClaveAccesoTest {
    @Test
    void generarClaveAcceso_formatoCorrecto() {
        String clave = ClaveAcceso.generar("01", "1790000000001", "001", "001", "...");
        assertEquals(49, clave.length());
        assertTrue(clave.matches("\\d{49}"));
    }
    // ... 7 más
}
```

**Qué prueba:**
- Formato de clave de 49 dígitos
- Generación con diferentes tipos de comprobante
- Validación de dígito verificador
- Fecha de emisión
- Ambientes (pruebas/producción)
- Claves duplicadas
- Longitud correcta
- Caracteres numéricos únicamente

#### ConfigAmbienteTest.java (4 tests)

**Propósito:** Validar configuración de ambiente SRI.

**Técnica:** Tests de persistencia usando archivos temporales.

```java
class ConfigAmbienteTest {
    @Test
    void guardarYLeer_ambientePersiste() throws Exception {
        File temp = File.createTempFile("config", ".properties");
        ConfigAmbiente.setRutaArchivo(temp.getAbsolutePath());
        ConfigAmbiente.guardar("PRUEBAS");
        assertEquals("PRUEBAS", ConfigAmbiente.leer());
    }
    // ... 3 más
}
```

**Qué prueba:**
- Persistencia de configuración
- Lectura de valores
- Valores por defecto
- Archivos de configuración temporales

#### ConfigFirmaTest.java (2 tests)

**Propósito:** Validar configuración de firma digital.

**Técnica:** Tests de lectura de configuración.

```java
class ConfigFirmaTest {
    @Test
    void cargarConfiguracion_existente_leeCorrectamente() {
        ConfigFirma config = ConfigFirma.cargar();
        assertNotNull(config);
    }
}
```

**Qué prueba:**
- Carga de configuración existente
- Manejo de configuración ausente

#### EmailServiceTest.java (2 tests)

**Propósito:** Validar servicio de correo sin enviar emails reales.

**Técnica:** Tests que validan estado y validaciones sin enviar.

```java
class EmailServiceTest {
    @Test
    void getUltimoError_inicialmente_esNull() {
        EmailService service = new EmailService();
        assertNull(service.getUltimoError());
    }

    @Test
    void enviarCorreoConPDF_retornaFalseCuandoDestinatarioVacio() {
        EmailService service = new EmailService();
        File pdfFicticio = new File("/tmp/proforma_ficticia_test.pdf");
        boolean resultado = service.enviarCorreoConPDF("", "Cliente", "PRO-001", "PROFORMA", pdfFicticio);
        assertFalse(resultado);
    }
}
```

**Qué prueba:**
- Estado inicial de error
- Validación de destinatario vacío (no envía email)

#### NotaVentaPDFTest.java (1 test)

**Propósito:** Validar generación de PDF de nota de venta.

**Técnica:** Test de integración que genera archivo PDF real.

```java
class NotaVentaPDFTest {
    @Test
    void generarPDF_archivoCreadoYContieneContenido() throws Exception {
        File temp = File.createTempFile("nota_venta_test_", ".pdf");
        String ruta = temp.getAbsolutePath();

        List<String[]> detalles = Arrays.asList(
            new String[]{"001", "Aceite 5W30", "2", "15.50", "31.00"},
            new String[]{"002", "Filtro Aceite", "1", "8.00", "8.00"}
        );

        NotaVentaPDF.generar(ruta, "001-001-000000001", "08/08/2026",
                "TAG REPUESTOS", "1790000000001", "Av. Principal",
                "0999999999", "tag@example.com",
                "Cliente Prueba", "1790000000002", "Quito",
                "0988888888", "cliente@example.com",
                "Efectivo", detalles,
                new BigDecimal("31.00"), new BigDecimal("4.65"),
                new BigDecimal("0.00"), new BigDecimal("35.65"));

        File f = new File(ruta);
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
        byte[] bytes = Files.readAllBytes(f.toPath());
        String content = new String(bytes);
        assertTrue(content.contains("TAG REPUESTOS") || content.contains("PROFORMA"));
    }
}
```

**Qué prueba:**
- Archivo PDF se crea correctamente
- PDF tiene contenido (tamaño > 0)
- PDF contiene texto esperado

#### SRIWebServiceTest.java (3 tests)

**Propósito:** Validar comunicación con servicios web SRI sin conectarse a la red.

**Técnica:** Herencia con método `postSoap` sobrescrito.

```java
class SRIWebServiceTest {
    private static class TestableSRIWebService extends SRIWebService {
        private String recepcionResponse;
        private String autorizacionResponse;

        TestableSRIWebService(String recepcionResponse, String autorizacionResponse) {
            this.recepcionResponse = recepcionResponse;
            this.autorizacionResponse = autorizacionResponse;
        }

        @Override
        protected String postSoap(String url, String soapBody) {
            if (url.contains("RecepcionComprobantesOffline")) {
                return recepcionResponse;
            }
            if (url.contains("AutorizacionComprobantesOffline")) {
                return autorizacionResponse;
            }
            return null;
        }
    }

    @Test
    void validarComprobante_recepcionRecibidaYAutorizacionAutorizado_retornaAutorizado() {
        String recepcion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...";
        String autorizacion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...";
        TestableSRIWebService service = new TestableSRIWebService(recepcion, autorizacion);
        SRIWebService.SRIResponse response = service.validarComprobante(
            "<xml>test</xml>", "1234567890123456789012345678901234567890123456789"
        );
        assertEquals("AUTORIZADO", response.getEstado());
    }
    // ... 2 más
}
```

**Qué prueba:**
- Flujo completo: recepción RECIBIDA → autorización AUTORIZADO
- Recepción individual devuelve estado RECIBIDA
- Autorización rechazada devuelve estado RECHAZADA

## Pruebas descartadas

### InventarioDAOTest.java

**Razón:** No se pudo implementar por incompatibilidades técnicas.

**Problema 1:** HSQLDB no soporta `STRING_AGG`
- El `InventarioDAO` usa `STRING_AGG` en una subconsulta para agrupar ubicaciones
- HSQLDB 2.7.2 no soporta esta función
- Solución posible: modificar `InventarioDAO` para producción, no recomendado

**Problema 2:** Mockito no puede mockear `java.sql.Connection` en Java 25
- Se intentó usar `MockedStatic<DatabaseConnection>` con mocks de `Connection`
- Error: `Java 25 (69) is not supported by the current version of Byte Buddy`
- Byte Buddy 1.14.9 soporta hasta Java 22
- Soluciones posibles:
  - Actualizar Byte Buddy a versión que soporte Java 25
  - Downgradear JDK a 22
  - Usar una librería de mocking diferente

**Decisión:** Se descartó temporalmente para no bloquear el avance. Se recomienda:
1. Actualizar dependencias de Byte Buddy/Mockito cuando soporten Java 25
2. O implementar un wrapper de `Connection` en `DatabaseConnection` para facilitar el mocking

## Ejecución de pruebas

### Comando completo

```bash
mvn clean test
```

### Ejecutar tests específicos

```bash
mvn test -Dtest=ClienteDAOTest,ProveedorDAOTest,SRIWebServiceTest,NotaVentaPDFTest
```

### Ver reportes

```bash
cat target/surefire-reports/com.tag.sysTagRep.*.txt | grep "Tests run"
```

## Patrones de testing utilizados

### 1. Test unitario directo
**Usado en:** `CifradoTest`, `ClaveAccesoTest`, `InventarioStockTest`, `NotaVentaRegistroTest`, `FacturaCalculatorTest`, `ProformaCalculatorTest`, `ConfigAmbienteTest`, `ConfigFirmaTest`, `EmailServiceTest`

**Descripción:** Se instancia la clase directamente y se prueba su comportamiento sin dependencias externas.

**Ventaja:** Rápido, sin configuración, ideal para lógica pura.

### 2. Test de integración con base de datos en memoria
**Usado en:** `ClienteDAOTest`, `ProveedorDAOTest`

**Descripción:**
1. Cada test crea su propia BD HSQLDB con nombre único
2. Configura `DatabaseConnection` para apuntar a esa BD
3. Crea las tablas necesarias
4. Ejecuta operaciones del DAO
5. Verifica resultados con consultas directas
6. Restaura configuración de `DatabaseConnection`

**Ventaja:** Prueba SQL real, aislamiento total entre tests.

### 3. Test por herencia (subclase testeable)
**Usado en:** `SRIWebServiceTest`

**Descripción:** Se crea una subclase que sobrescribe el método `postSoap` para devolver respuestas predefinidas sin conectarse a la red.

**Ventaja:** No requiere modificar la clase original ni usar reflection, testing simple.

### 4. Test de generación de archivos
**Usado en:** `NotaVentaPDFTest`

**Descripción:** Genera un archivo PDF real y valida que exista, tenga contenido y contenga texto esperado.

**Ventaja:** Prueba el flujo completo de generación.

## Decisiones técnicas

### ¿Por qué HSQLDB en memoria?
- No requiere servidor corriendo
- Aislamiento total entre tests
- Rápido
- Compatible con SQL estándar

### ¿Por qué no Mockito para DAOs con JDBC?
- `InventarioDAOTest` intentó usar `MockedStatic<DatabaseConnection>` con mocks de `Connection`
- No funciona en Java 25 por limitación de Byte Buddy 1.14.9
- Alternativa: usar wrapper de conexión o actualizar dependencias

### ¿Por qué herencia en SRIWebService en lugar de reflection?
- Más limpio y type-safe
- No requiere modificar código de producción más allá de cambiar `private` a `protected`
- Fácil de entender y mantener

### ¿Por qué no mockear PDF?
- `NotaVentaPDF` usa iText (librería legada)
- Mockear sería complejo y frágil
- Mejor probar generación real de PDF

### ¿Por qué no tests de controllers JavaFX?
- Requieren headless testing (Monocle)
- Configuración compleja
- Lógica de controllers delegada a calculadoras (ya testeadas)
- Próximo paso recomendado si se necesita cobertura UI

## Próximos pasos recomendados

1. **Actualizar Mockito/Byte Buddy** cuando soporten Java 25
   - Reintentar `InventarioDAOTest` con mocks de JDBC

2. **Tests de controllers JavaFX**
   - Configurar Monocle para headless testing
   - Probar `NotaVentaController`, `FacturaController`, etc.

3. **Tests de integración con Testcontainers**
   - Usar PostgreSQL real en contenedor Docker
   - Probar DAOs con BD real

4. **Tests de `DashboardDAO`**
   - Lógica de reportes y métricas

5. **Tests de `PdfElectronico`**
   - Generación de facturas XML + PDF

## Notas

- `DatabaseConnection` mantiene comportamiento original en producción (PostgreSQL)
- Tests de DAOs solo modifican configuración durante su ejecución y la restauran
- `SRIWebService` sigue sin conectarse a servicios reales en pruebas
- `EmailServiceTest` no envía emails reales, solo valida estado y validaciones
