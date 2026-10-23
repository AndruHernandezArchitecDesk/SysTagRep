package com.vendex.controller;

import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Animaciones locales de Vetxi. Todo el estado y las transiciones pertenecen al hilo FX. */
final class VetxiAnimaciones implements MascotaAnimaciones {
    enum Estado { REPOSO, RASCANDO, ANOTANDO, PASEANDO }
    static final long INACTIVIDAD_NANOS = java.time.Duration.ofSeconds(60).toNanos();

    private final Group dibujo;
    private final Region mascota, superficie;
    private final Node lentes, libreta, lapiz, pataRascado, pulgas, pataTrasera, pataDelantera;
    private final BooleanSupplier libre;
    private final BiConsumer<Double, Double> mover;
    private final LongSupplier reloj;
    private final Timeline vigilancia;
    private final ScaleTransition respiracion;
    private Animation accion, escritura, aviso;
    private Timeline paseo;
    private Estado estado = Estado.REPOSO;
    private boolean activo, trabajando;
    private long ultimaActividad, proximoRascado, proximoPaseo;

    VetxiAnimaciones(Group dibujo, Region mascota, Region superficie,
                     BooleanSupplier libre, BiConsumer<Double, Double> mover) {
        this(dibujo, mascota, superficie, libre, mover, System::nanoTime);
    }

    VetxiAnimaciones(Group dibujo, Region mascota, Region superficie,
                     BooleanSupplier libre, BiConsumer<Double, Double> mover, LongSupplier reloj) {
        this.dibujo = dibujo; this.mascota = mascota; this.superficie = superficie;
        this.libre = libre; this.mover = mover; this.reloj = reloj;
        lentes = nodo("lentes"); libreta = nodo("libreta"); lapiz = nodo("lapiz");
        pataRascado = nodo("pataRascado"); pulgas = nodo("pulgas");
        pataTrasera = nodo("pataTrasera"); pataDelantera = nodo("pataDelantera");
        respiracion = new ScaleTransition(Duration.seconds(2.2), dibujo);
        respiracion.setFromY(1); respiracion.setToY(1.035);
        respiracion.setAutoReverse(true); respiracion.setCycleCount(Animation.INDEFINITE);
        vigilancia = new Timeline(new KeyFrame(Duration.seconds(1), e -> comprobarInactividad()));
        vigilancia.setCycleCount(Animation.INDEFINITE);
        ultimaActividad = reloj.getAsLong();
        programarRascado();
        mostrarPose();
    }

    private Node nodo(String id) {
        Node nodo = dibujo.lookup("#" + id);
        if (nodo == null) throw new IllegalStateException("Falta la pieza animada de Vetxi: " + id);
        return nodo;
    }

    Estado estado() { return estado; }

    public void setActivo(boolean valor) {
        if (activo == valor) return;
        activo = valor;
        if (valor) {
            ultimaActividad = reloj.getAsLong();
            programarRascado();
            vigilancia.playFromStart(); respiracion.play();
            mostrarPose();
        } else {
            vigilancia.stop(); respiracion.stop();
            detenerAccion();
            if (aviso != null) aviso.stop();
            mascota.setScaleX(1); mascota.setScaleY(1);
            mostrarPose();
        }
    }

    public void setTrabajando(boolean valor) {
        if (trabajando == valor) return;
        trabajando = valor;
        detenerAccion();
        ultimaActividad = reloj.getAsLong();
        mostrarPose();
    }

    public void registrarActividad() {
        ultimaActividad = reloj.getAsLong();
        if (estado == Estado.PASEANDO || estado == Estado.RASCANDO) {
            detenerAccion();
            programarRascado();
            mostrarPose();
        }
    }

    void comprobarInactividad() {
        if (!activo) return;
        if (!libre.getAsBoolean()) {
            if (estado == Estado.PASEANDO || estado == Estado.RASCANDO) { detenerAccion(); mostrarPose(); }
            return;
        }
        long ahora = reloj.getAsLong();
        if (estado == Estado.PASEANDO || estado == Estado.RASCANDO) return;
        if (ahora - ultimaActividad >= INACTIVIDAD_NANOS && ahora >= proximoPaseo) {
            iniciarPaseo();
        } else if (!trabajando && ahora >= proximoRascado) rascar();
    }

    private void programarRascado() {
        proximoRascado = reloj.getAsLong() + java.time.Duration.ofSeconds(
                ThreadLocalRandom.current().nextLong(18, 33)).toNanos();
    }

    private RotateTransition balanceo(Node nodo, double desde, double hasta, double ms, int ciclos) {
        var r = new RotateTransition(Duration.millis(ms), nodo);
        r.setFromAngle(desde); r.setToAngle(hasta); r.setAutoReverse(true); r.setCycleCount(ciclos);
        return r;
    }

    void rascar() {
        if (!activo || trabajando || !libre.getAsBoolean() || estado == Estado.PASEANDO) return;
        detenerAccion();
        estado = Estado.RASCANDO;
        mostrarPose();
        var saltoPulgas = new TranslateTransition(Duration.millis(180), pulgas);
        saltoPulgas.setFromY(0); saltoPulgas.setToY(-5);
        saltoPulgas.setAutoReverse(true); saltoPulgas.setCycleCount(10);
        accion = new ParallelTransition(balanceo(pataRascado, -18, 24, 90, 20),
                balanceo(dibujo, -2, 2, 180, 10), saltoPulgas);
        accion.setOnFinished(e -> {
            detenerAccion(); programarRascado(); mostrarPose();
        });
        accion.play();
    }

    private void mostrarPose() {
        boolean escribiendo = trabajando && estado != Estado.PASEANDO && estado != Estado.RASCANDO;
        lentes.setVisible(escribiendo); libreta.setVisible(escribiendo); lapiz.setVisible(escribiendo);
        pataRascado.setVisible(estado == Estado.RASCANDO); pulgas.setVisible(estado == Estado.RASCANDO);
        pataTrasera.setVisible(estado != Estado.RASCANDO);
        if (escritura != null) { escritura.stop(); escritura = null; }
        if (estado != Estado.PASEANDO && estado != Estado.RASCANDO) {
            estado = escribiendo ? Estado.ANOTANDO : Estado.REPOSO;
        }
        if (escribiendo && activo) {
            var trazo = new TranslateTransition(Duration.millis(150), lapiz);
            trazo.setFromX(0); trazo.setToX(3); trazo.setFromY(0); trazo.setToY(-2);
            trazo.setAutoReverse(true); trazo.setCycleCount(8);
            var escribir = new ParallelTransition(trazo, balanceo(lapiz, -8, 8, 150, 8));
            escritura = new SequentialTransition(escribir, new PauseTransition(Duration.seconds(1.8)));
            escritura.setCycleCount(Animation.INDEFINITE);
            escritura.play();
        }
    }

    private void iniciarPaseo() {
        double maxX = Math.max(0, superficie.getWidth() - 104);
        double maxY = Math.max(0, superficie.getHeight() - 108);
        if (maxX < 40 || maxY < 40) return;
        double inicioX = mascota.getLayoutX(), inicioY = mascota.getLayoutY();
        double finX = ThreadLocalRandom.current().nextDouble(8, Math.max(9, maxX - 8));
        double finY = ThreadLocalRandom.current().nextDouble(8, Math.max(9, maxY - 8));
        if (Math.hypot(finX - inicioX, finY - inicioY) < 40) {
            finX = inicioX < maxX / 2 ? maxX - 8 : 8;
            finY = inicioY < maxY / 2 ? maxY - 8 : 8;
        }
        detenerAccion();
        estado = Estado.PASEANDO;
        mostrarPose();
        dibujo.setScaleX(finX < inicioX ? -1 : 1);
        var pataA = paso(pataTrasera, -2, 2);
        var pataB = paso(pataDelantera, 2, -2);
        var cuerpo = new TranslateTransition(Duration.millis(240), dibujo);
        cuerpo.setFromY(0); cuerpo.setToY(-2); cuerpo.setAutoReverse(true); cuerpo.setCycleCount(Animation.INDEFINITE);
        accion = new ParallelTransition(pataA, pataB, cuerpo);
        accion.play();
        final double destinoX = finX, destinoY = finY;
        DoubleProperty progreso = new SimpleDoubleProperty(0);
        progreso.addListener((o, a, b) -> mover.accept(inicioX + (destinoX - inicioX) * b.doubleValue(),
                inicioY + (destinoY - inicioY) * b.doubleValue()));
        double segundos = Math.max(2, Math.hypot(finX - inicioX, finY - inicioY) / 45);
        paseo = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(progreso, 0)),
                new KeyFrame(Duration.seconds(segundos), new KeyValue(progreso, 1, Interpolator.LINEAR)));
        paseo.setOnFinished(e -> {
            detenerAccion();
            proximoPaseo = reloj.getAsLong() + java.time.Duration.ofSeconds(2).toNanos();
            mostrarPose();
        });
        paseo.play();
    }

    private TranslateTransition paso(Node pata, double desde, double hasta) {
        var paso = new TranslateTransition(Duration.millis(240), pata);
        paso.setFromX(desde); paso.setToX(hasta); paso.setFromY(0); paso.setToY(-2);
        paso.setAutoReverse(true); paso.setCycleCount(Animation.INDEFINITE);
        return paso;
    }

    private void detenerAccion() {
        double x = mascota.getLayoutX(), y = mascota.getLayoutY();
        boolean caminaba = estado == Estado.PASEANDO;
        if (paseo != null) { paseo.stop(); paseo = null; }
        if (accion != null) { accion.stop(); accion = null; }
        if (escritura != null) { escritura.stop(); escritura = null; }
        for (Node n : new Node[]{dibujo, pataTrasera, pataDelantera, pataRascado, pulgas, lapiz}) {
            n.setTranslateX(0); n.setTranslateY(0); n.setRotate(0);
        }
        dibujo.setScaleX(1);
        estado = Estado.REPOSO;
        if (caminaba) mover.accept(x, y); // Detenerse donde estaba; no saltar ni guardar posición automática.
    }

    public void avisar() {
        if (estado == Estado.PASEANDO || estado == Estado.RASCANDO) { detenerAccion(); mostrarPose(); }
        if (!activo) return;
        if (aviso != null) aviso.stop();
        var saludo = new ScaleTransition(Duration.millis(180), mascota);
        saludo.setFromX(1); saludo.setToX(1.05); saludo.setFromY(1); saludo.setToY(1.05);
        saludo.setCycleCount(4); saludo.setAutoReverse(true);
        saludo.setOnFinished(e -> { mascota.setScaleX(1); mascota.setScaleY(1); });
        aviso = saludo; saludo.play();
    }
}
