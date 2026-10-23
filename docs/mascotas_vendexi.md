# Mascotas del asistente

El botón flotante permite elegir **Vendexi (robot)** o **Vetxi (capibara)** mediante
clic derecho (también con el menú contextual de teclado del botón). La elección
se guarda en Preferences, nodo `/com/tag/vendex/asistente`, clave `mascota`.
Sin una preferencia guardada se utiliza el robot.

El robot es una interpretación vectorial FXML de la referencia aprobada, con
carcasa nacarada, visor oscuro, arcos cyan, módulos laterales y V azul/violeta.
No es el render 3D rasterizado original: usa gradientes y formas JavaFX sobre
fondo transparente para poder animar sus piezas.

## Recursos y comportamiento

- `src/main/resources/view/RobotVendexi.fxml`: dibujo y piezas articuladas.
- `src/main/java/com/vendex/controller/VendexiAnimaciones.java`: flotación suave,
  halo, saludo de brazo, escaneo y holograma al trabajar en factura/proforma;
  desplazamiento tras 60 segundos sin actividad.
- `MascotaAnimaciones.java`: contrato de ciclo de vida compartido.
- `VetxiAnimaciones.java` y `view/Capibara.fxml`: motor y dibujo del capibara.
- `ChatWidgetController.java`: selector, carga desde classpath y persistencia;
  detiene el motor anterior antes de intercambiar mascotas.
- `ChatWidget.fxml`, `MainView.fxml` y `css/chat-widget.css`: textos neutros y
  acentos navy, cyan, azul y violeta.

Las animaciones se detienen al perder foco u ocultar la ventana. La interacción
interrumpe el paseo; el desplazamiento automático no guarda una nueva posición.
El nombre conversacional sigue siendo Vetxi y cambiar mascota conserva el chat.

Verificación: `mvn test -Dtest='Vetxi*,Vendexi*'`.
