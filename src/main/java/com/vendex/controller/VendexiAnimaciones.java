package com.vendex.controller;

import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import java.util.function.BooleanSupplier;

/** Animación de piezas reales del robot vectorial, sin alterar la posición guardada. */
final class VendexiAnimaciones implements MascotaAnimaciones {
    private final Group dibujo;
    private final Node escaneo, holograma;
    private final Rotate brazo;
    private final BooleanSupplier libre;
    private final Animation reposo, analisis;
    private final Timeline vigilancia;
    private Animation saludo;
    private boolean activo, trabajando;
    private long ultimaActividad = System.nanoTime();
    private Timeline paseo;
    private final javafx.scene.layout.Region mascota, superficie;
    private final java.util.function.BiConsumer<Double, Double> mover;

    VendexiAnimaciones(Group dibujo, BooleanSupplier libre, javafx.scene.layout.Region mascota,
                       javafx.scene.layout.Region superficie, java.util.function.BiConsumer<Double, Double> mover) {
        this.mascota = mascota; this.superficie = superficie; this.mover = mover;
        this.dibujo = dibujo;
        this.libre = libre;
        escaneo = dibujo.lookup("#escaneo");
        holograma = dibujo.lookup("#holograma");
        brazo = (Rotate) dibujo.lookup("#brazoSaludo").getTransforms().get(0);
        var flotar = new TranslateTransition(Duration.seconds(2.4), dibujo);
        flotar.setFromY(0); flotar.setToY(-2);
        flotar.setAutoReverse(true); flotar.setCycleCount(Animation.INDEFINITE);
        var luz = new FadeTransition(Duration.seconds(2.4), dibujo.lookup("#halo"));
        luz.setFromValue(0.2); luz.setToValue(0.6);
        luz.setAutoReverse(true); luz.setCycleCount(Animation.INDEFINITE);
        reposo = new ParallelTransition(flotar, luz);
        var barrido = new TranslateTransition(Duration.seconds(1.8), escaneo);
        barrido.setFromY(0); barrido.setToY(17);
        barrido.setAutoReverse(true); barrido.setCycleCount(Animation.INDEFINITE);
        analisis = barrido;
        vigilancia = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!libre.getAsBoolean()) { detenerPaseo(); return; }
            long segundos = (System.nanoTime() - ultimaActividad) / 1_000_000_000L;
            if (segundos >= 60 && paseo == null) pasear();
            else if (segundos > 0 && segundos % 24 == 0 && paseo == null) avisar();
        }));
        vigilancia.setCycleCount(Animation.INDEFINITE);
    }

    public void setActivo(boolean valor) {
        if (activo == valor) return;
        activo = valor;
        ultimaActividad = System.nanoTime();
        if (valor) { reposo.play(); vigilancia.play(); if (trabajando) analisis.play(); }
        else {
            reposo.stop(); vigilancia.stop(); analisis.stop(); detenerSaludo(); detenerPaseo();
            dibujo.setTranslateY(0); escaneo.setTranslateY(0);
        }
    }

    public void setTrabajando(boolean valor) {
        trabajando = valor;
        escaneo.setVisible(valor); holograma.setVisible(valor);
        analisis.stop(); escaneo.setTranslateY(0);
        if (valor && activo) analisis.playFromStart();
    }

    public void registrarActividad() {
        ultimaActividad = System.nanoTime(); detenerSaludo(); detenerPaseo();
        if (activo) vigilancia.playFromStart();
    }

    private void detenerPaseo() {
        if (paseo != null) { paseo.stop(); paseo = null; }
    }

    private void pasear() {
        double maxX = superficie.getWidth() - mascota.getWidth();
        double maxY = superficie.getHeight() - mascota.getHeight();
        if (maxX < 40 || maxY < 40) return;
        double x = mascota.getLayoutX(), y = mascota.getLayoutY();
        double destinoX = java.util.concurrent.ThreadLocalRandom.current().nextDouble(8, maxX - 8);
        double destinoY = java.util.concurrent.ThreadLocalRandom.current().nextDouble(8, maxY - 8);
        var progreso = new javafx.beans.property.SimpleDoubleProperty();
        progreso.addListener((o, a, b) -> mover.accept(x + (destinoX - x) * b.doubleValue(), y + (destinoY - y) * b.doubleValue()));
        paseo = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(progreso, 0)),
                new KeyFrame(Duration.seconds(Math.max(2, Math.hypot(destinoX - x, destinoY - y) / 45)),
                        new KeyValue(progreso, 1, Interpolator.EASE_BOTH)));
        paseo.setOnFinished(e -> { paseo = null; ultimaActividad = System.nanoTime(); });
        paseo.play();
    }

    private void detenerSaludo() {
        if (saludo != null) saludo.stop();
        brazo.setAngle(0);
    }

    public void avisar() {
        if (!activo) return;
        detenerSaludo();
        saludo = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(brazo.angleProperty(), 0)),
                new KeyFrame(Duration.millis(250), new KeyValue(brazo.angleProperty(), -16, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(500), new KeyValue(brazo.angleProperty(), 0, Interpolator.EASE_BOTH)));
        saludo.setCycleCount(3); saludo.play();
    }
}
