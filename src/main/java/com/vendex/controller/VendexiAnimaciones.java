package com.vendex.controller;

import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Animaciones propias de Vetxi: gestos en reposo y pose de portátil al facturar. */
final class VendexiAnimaciones implements MascotaAnimaciones {
    private static final double BRAZO_RELAX = 42;
    private final Group dibujo;
    private final Node halo, cabeza, ojos, sonrisa, computadora, teclas, escaneo, holograma, brazoTeclado;
    private final Rotate brazoSaludo, giroCabeza, giroTeclado;
    private final BooleanSupplier libre;
    private final javafx.scene.layout.Region mascota, superficie;
    private final BiConsumer<Double, Double> mover;
    private final Animation respiracion;
    private final Timeline vigilancia;
    private PauseTransition esperaParpadeo, esperaGesto;
    private Animation parpadeo, gesto, saludo, escritura, mostrarPortatil;
    private Timeline paseo;
    private boolean activo, trabajando;
    private long ultimaActividad = System.nanoTime();

    VendexiAnimaciones(Group dibujo, BooleanSupplier libre, javafx.scene.layout.Region mascota,
                       javafx.scene.layout.Region superficie, BiConsumer<Double, Double> mover) {
        this.dibujo = dibujo;
        this.libre = libre;
        this.mascota = mascota;
        this.superficie = superficie;
        this.mover = mover;
        halo = nodo("halo"); cabeza = nodo("cabeza"); ojos = nodo("ojos"); sonrisa = nodo("sonrisa");
        computadora = nodo("computadora"); teclas = nodo("teclas"); escaneo = nodo("escaneo");
        holograma = nodo("holograma"); brazoTeclado = nodo("brazoTeclado");
        brazoSaludo = transform("brazoSaludo");
        giroCabeza = transform("cabeza");
        giroTeclado = transform("brazoTeclado");

        var flotar = new TranslateTransition(Duration.seconds(2.4), dibujo);
        flotar.setFromY(0); flotar.setToY(-2.5);
        flotar.setAutoReverse(true); flotar.setCycleCount(Animation.INDEFINITE);
        var luz = new FadeTransition(Duration.seconds(2.2), halo);
        luz.setFromValue(0.18); luz.setToValue(0.62);
        luz.setAutoReverse(true); luz.setCycleCount(Animation.INDEFINITE);
        var respiracionCabeza = new ScaleTransition(Duration.seconds(2.8), cabeza);
        respiracionCabeza.setFromX(1); respiracionCabeza.setToX(1.012);
        respiracionCabeza.setFromY(1); respiracionCabeza.setToY(1.018);
        respiracionCabeza.setAutoReverse(true); respiracionCabeza.setCycleCount(Animation.INDEFINITE);
        respiracion = new ParallelTransition(flotar, luz, respiracionCabeza);
        vigilancia = new Timeline(new KeyFrame(Duration.seconds(1), e -> vigilarInactividad()));
        vigilancia.setCycleCount(Animation.INDEFINITE);
        dejarPoseReposo();
    }

    private Node nodo(String id) {
        Node n = dibujo.lookup("#" + id);
        if (n == null) throw new IllegalStateException("Falta la pieza animada de Vendexi: " + id);
        return n;
    }

    private Rotate transform(String id) {
        Node n = nodo(id);
        for (var transform : n.getTransforms()) if (transform instanceof Rotate r) return r;
        throw new IllegalStateException("Falta el pivote de rotación de Vendexi: " + id);
    }

    @Override public void setActivo(boolean valor) {
        if (activo == valor) return;
        activo = valor;
        ultimaActividad = System.nanoTime();
        if (valor) {
            respiracion.play(); vigilancia.playFromStart();
            if (trabajando) iniciarTrabajo(); else iniciarVidaEnReposo();
        } else {
            vigilancia.stop(); respiracion.stop(); detenerTimersVida();
            detenerPaseo(); detenerSaludo(); detenerGesto(); detenerEscritura();
            if (mostrarPortatil != null) { mostrarPortatil.stop(); mostrarPortatil = null; }
            dibujo.setTranslateY(0); halo.setOpacity(0.3); ojos.setScaleY(1); ojos.setTranslateX(0);
            cabeza.setScaleX(1); cabeza.setScaleY(1); giroCabeza.setAngle(0);
            computadora.setVisible(trabajando);
            dejarPoseReposo();
            if (trabajando) fijarPoseTrabajo();
        }
    }

    @Override public void setTrabajando(boolean valor) {
        if (trabajando == valor) return;
        trabajando = valor;
        ultimaActividad = System.nanoTime();
        detenerTimersVida(); detenerSaludo(); detenerGesto(); detenerEscritura();
        detenerPaseo();
        if (valor) {
            if (activo) iniciarTrabajo(); else { fijarPoseTrabajo(); computadora.setVisible(true); }
        } else {
            cerrarPortatil(); dejarPoseReposo();
            if (activo) { respiracion.playFromStart(); iniciarVidaEnReposo(); }
        }
    }

    @Override public void registrarActividad() {
        ultimaActividad = System.nanoTime();
        detenerPaseo();
        // Los gestos de Vetxi son vida propia: la actividad del usuario no los
        // reinicia continuamente ni deja los brazos congelados durante el uso normal.
        if (activo && !trabajando && gesto == null && saludo == null && esperaGesto == null) programarGesto();
        if (activo) vigilancia.playFromStart();
    }

    private void iniciarVidaEnReposo() {
        if (!activo || trabajando) return;
        programarParpadeo(); programarGesto();
    }

    private void iniciarTrabajo() {
        if (!activo || !trabajando) return;
        detenerTimersVida(); detenerGesto(); detenerSaludo();
        detenerEscritura();
        respiracion.stop();
        dibujo.setTranslateY(0);
        cabeza.setScaleX(1); cabeza.setScaleY(1);
        escaneo.setVisible(false); holograma.setVisible(false);
        computadora.setVisible(true);
        computadora.setScaleX(1); computadora.setScaleY(1);
        var brillo = new FadeTransition(Duration.millis(600), computadora);
        brillo.setFromValue(0); brillo.setToValue(1);
        mostrarPortatil = brillo;
        mostrarPortatil.playFromStart();
        fijarPoseTrabajo();

        // Lectura tranquila de la pantalla: brazos y teclado inmóviles. Un único
        // timeline gobierna cabeza/ojos sin competir con saludos o tecleo.
        escritura = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(giroCabeza.angleProperty(), -3),
                        new KeyValue(ojos.scaleYProperty(), 0.85)),
                new KeyFrame(Duration.seconds(3), new KeyValue(giroCabeza.angleProperty(), -4.5, Interpolator.EASE_BOTH),
                        new KeyValue(ojos.scaleYProperty(), 0.85)),
                new KeyFrame(Duration.seconds(3.12), new KeyValue(ojos.scaleYProperty(), 0.1, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(3.3), new KeyValue(ojos.scaleYProperty(), 0.85, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(6), new KeyValue(giroCabeza.angleProperty(), -3, Interpolator.EASE_BOTH),
                        new KeyValue(ojos.scaleYProperty(), 0.85)));
        escritura.setCycleCount(Animation.INDEFINITE);
        escritura.playFromStart();
    }

    private void fijarPoseTrabajo() {
        computadora.setVisible(true); escaneo.setVisible(false); holograma.setVisible(false);
        giroCabeza.setAngle(-3); brazoSaludo.setAngle(BRAZO_RELAX); giroTeclado.setAngle(-4);
        ojos.setScaleY(0.85); teclas.setTranslateY(0);
    }

    private void cerrarPortatil() {
        if (mostrarPortatil != null) { mostrarPortatil.stop(); mostrarPortatil = null; }
        computadora.setVisible(false); computadora.setOpacity(1); computadora.setScaleX(1); computadora.setScaleY(1);
        teclas.setTranslateY(0); giroTeclado.setAngle(0);
    }

    private void dejarPoseReposo() {
        escaneo.setVisible(false); holograma.setVisible(false);
        giroCabeza.setAngle(0); brazoSaludo.setAngle(BRAZO_RELAX); giroTeclado.setAngle(0);
        ojos.setScaleY(1); ojos.setTranslateX(0); sonrisa.setScaleX(1); sonrisa.setScaleY(1);
        teclas.setTranslateY(0);
        if (!trabajando) computadora.setVisible(false);
    }

    private void programarParpadeo() {
        if (!activo || trabajando) return;
        if (esperaParpadeo != null) esperaParpadeo.stop();
        esperaParpadeo = new PauseTransition(Duration.seconds(ThreadLocalRandom.current().nextDouble(2.8, 5.7)));
        esperaParpadeo.setOnFinished(e -> {
            if (!activo || trabajando) return;
            var cerrar = new ScaleTransition(Duration.millis(85), ojos);
            cerrar.setToY(0.08);
            var abrir = new ScaleTransition(Duration.millis(115), ojos);
            abrir.setToY(1);
            parpadeo = new SequentialTransition(cerrar, abrir);
            parpadeo.setOnFinished(fin -> { ojos.setScaleY(1); programarParpadeo(); });
            parpadeo.playFromStart();
        });
        esperaParpadeo.playFromStart();
    }

    private void programarGesto() {
        if (!activo || trabajando) return;
        if (esperaGesto != null) esperaGesto.stop();
        esperaGesto = new PauseTransition(Duration.seconds(ThreadLocalRandom.current().nextDouble(3.5, 6.5)));
        esperaGesto.setOnFinished(e -> hacerGestoAleatorio());
        esperaGesto.playFromStart();
    }

    private void hacerGestoAleatorio() {
        if (!activo || trabajando) return;
        if (paseo != null || saludo != null) { programarGesto(); return; }
        switch (ThreadLocalRandom.current().nextInt(3)) {
            case 0 -> cambiarPoseBrazos();
            case 1 -> { if (ThreadLocalRandom.current().nextBoolean()) asentir(); else mirarAlrededor(); }
            default -> avisar();
        }
    }

    /** Cambia entre poses de saludo/atención y vuelve suavemente al reposo. */
    private void cambiarPoseBrazos() {
        gesto = new Timeline(new KeyFrame(Duration.ZERO,
                        new KeyValue(brazoSaludo.angleProperty(), BRAZO_RELAX), new KeyValue(giroTeclado.angleProperty(), 0)),
                new KeyFrame(Duration.millis(260),
                        new KeyValue(brazoSaludo.angleProperty(), BRAZO_RELAX - 26, Interpolator.EASE_BOTH),
                        new KeyValue(giroTeclado.angleProperty(), 11, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(560),
                        new KeyValue(brazoSaludo.angleProperty(), BRAZO_RELAX - 9, Interpolator.EASE_BOTH),
                        new KeyValue(giroTeclado.angleProperty(), -9, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(850),
                        new KeyValue(brazoSaludo.angleProperty(), BRAZO_RELAX + 5, Interpolator.EASE_BOTH),
                        new KeyValue(giroTeclado.angleProperty(), -15, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1180),
                        new KeyValue(brazoSaludo.angleProperty(), BRAZO_RELAX, Interpolator.EASE_BOTH),
                        new KeyValue(giroTeclado.angleProperty(), 0, Interpolator.EASE_BOTH)));
        gesto.setOnFinished(e -> {
            brazoSaludo.setAngle(BRAZO_RELAX); giroTeclado.setAngle(0); gesto = null; programarGesto();
        });
        gesto.playFromStart();
    }

    private void asentir() {
        giroCabeza.setAngle(0);
        gesto = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(giroCabeza.angleProperty(), 0)),
                new KeyFrame(Duration.millis(260), new KeyValue(giroCabeza.angleProperty(), 5, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(520), new KeyValue(giroCabeza.angleProperty(), -3, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(820), new KeyValue(giroCabeza.angleProperty(), 0, Interpolator.EASE_BOTH)));
        gesto.setOnFinished(e -> { giroCabeza.setAngle(0); gesto = null; programarGesto(); });
        gesto.playFromStart();
    }

    private void mirarAlrededor() {
        var mirada = new TranslateTransition(Duration.millis(360), ojos);
        mirada.setFromX(0); mirada.setToX(ThreadLocalRandom.current().nextBoolean() ? 1.6 : -1.6);
        mirada.setAutoReverse(true); mirada.setCycleCount(2);
        var sonrisaTr = new ScaleTransition(Duration.millis(360), sonrisa);
        sonrisaTr.setFromX(1); sonrisaTr.setToX(1.15); sonrisaTr.setAutoReverse(true); sonrisaTr.setCycleCount(2);
        gesto = new ParallelTransition(mirada, sonrisaTr);
        gesto.setOnFinished(e -> { ojos.setTranslateX(0); sonrisa.setScaleX(1); gesto = null; programarGesto(); });
        gesto.playFromStart();
    }

    private void detenerTimersVida() {
        if (esperaParpadeo != null) esperaParpadeo.stop();
        if (esperaGesto != null) esperaGesto.stop();
        if (parpadeo != null) parpadeo.stop();
    }

    private void detenerGesto() {
        if (gesto != null) gesto.stop();
        gesto = null; giroCabeza.setAngle(trabajando ? -3 : 0);
        brazoSaludo.setAngle(BRAZO_RELAX);
        giroTeclado.setAngle(trabajando ? -4 : 0);
        ojos.setTranslateX(0); sonrisa.setScaleX(1); sonrisa.setScaleY(1);
    }

    private void detenerEscritura() {
        if (escritura != null) escritura.stop();
        escritura = null; teclas.setTranslateY(0);
    }

    private void vigilarInactividad() {
        if (!libre.getAsBoolean() || trabajando) { detenerPaseo(); return; }
        if (paseo != null) return;
        long segundos = (System.nanoTime() - ultimaActividad) / 1_000_000_000L;
        if (segundos >= 60) pasear();
    }

    private void detenerPaseo() {
        if (paseo != null) { paseo.stop(); paseo = null; }
    }

    private void pasear() {
        double maxX = superficie.getWidth() - mascota.getWidth();
        double maxY = superficie.getHeight() - mascota.getHeight();
        if (maxX < 40 || maxY < 40) return;
        double x = mascota.getLayoutX(), y = mascota.getLayoutY();
        double destinoX = ThreadLocalRandom.current().nextDouble(8, maxX - 8);
        double destinoY = ThreadLocalRandom.current().nextDouble(8, maxY - 8);
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
        saludo = null; brazoSaludo.setAngle(BRAZO_RELAX); sonrisa.setScaleX(1);
    }

    @Override public void avisar() {
        if (!activo) return;
        if (trabajando) return; // Mantener la concentración sin sacudir los brazos.
        detenerGesto(); detenerSaludo();
        boolean reanudarEscritura = trabajando && escritura != null && escritura.getStatus() == Animation.Status.RUNNING;
        if (reanudarEscritura) escritura.pause();
        double reposoBrazo = BRAZO_RELAX;
        var balanceo = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(brazoSaludo.angleProperty(), reposoBrazo)),
                new KeyFrame(Duration.millis(230), new KeyValue(brazoSaludo.angleProperty(), reposoBrazo - 18, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(460), new KeyValue(brazoSaludo.angleProperty(), reposoBrazo, Interpolator.EASE_BOTH)));
        balanceo.setCycleCount(3);
        var sonrisaGrande = new ScaleTransition(Duration.millis(230), sonrisa);
        sonrisaGrande.setFromX(1); sonrisaGrande.setToX(1.2); sonrisaGrande.setAutoReverse(true); sonrisaGrande.setCycleCount(6);
        saludo = new ParallelTransition(balanceo, sonrisaGrande);
        saludo.setOnFinished(e -> {
            saludo = null; brazoSaludo.setAngle(reposoBrazo); sonrisa.setScaleX(1);
            if (reanudarEscritura && escritura != null) escritura.play();
            if (!trabajando) programarGesto();
        });
        saludo.playFromStart();
    }
}
