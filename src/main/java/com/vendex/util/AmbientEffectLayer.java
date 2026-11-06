package com.vendex.util;

import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Lightweight, mouse-transparent particles kept in narrow bands along window edges. */
final class AmbientEffectLayer extends Pane {
    private static final Random RANDOM = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private final AnimationTimer timer;
    private ThemeManager.Theme theme = ThemeManager.Theme.ORGANIC;
    private ThemeManager.AmbientIntensity intensity = ThemeManager.AmbientIntensity.LOW;
    private ThemeManager.AmbientSpeed speed = ThemeManager.AmbientSpeed.SLOW;
    private boolean enabled;
    private long lastFrame;
    private boolean running;
    private int slowFrames;

    AmbientEffectLayer() {
        setManaged(false);
        setMouseTransparent(true);
        setPickOnBounds(false);
        setStyle("-fx-background-color: transparent;");
        setClip(new Rectangle());
        widthProperty().addListener((obs, oldValue, newValue) -> resizeParticles());
        heightProperty().addListener((obs, oldValue, newValue) -> resizeParticles());
        timer = new AnimationTimer() {
            @Override public void handle(long now) { animate(now); }
        };
    }

    void configure(ThemeManager.Theme selectedTheme, boolean enabled,
                   ThemeManager.AmbientIntensity selectedIntensity,
                   ThemeManager.AmbientSpeed selectedSpeed) {
        theme = selectedTheme == null ? ThemeManager.Theme.ORGANIC : selectedTheme;
        intensity = selectedIntensity == null ? ThemeManager.AmbientIntensity.LOW : selectedIntensity;
        speed = selectedSpeed == null ? ThemeManager.AmbientSpeed.SLOW : selectedSpeed;
        this.enabled = enabled;
        stopAnimation();
        getChildren().clear();
        particles.clear();
        buildParticles();
    }

    void suspend() {
        stopAnimation();
        getChildren().clear();
        particles.clear();
    }

    void resume() {
        if (enabled && isAnimatedTheme(theme) && particles.isEmpty()) buildParticles();
    }

    private void buildParticles() {
        slowFrames = 0;
        if (!enabled || !isAnimatedTheme(theme)) return;

        int count = switch (intensity) {
            case LOW -> 6;
            case MEDIUM -> 11;
            case HIGH -> 17;
        };
        for (int i = 0; i < count; i++) {
            Particle particle = createParticle(i);
            particles.add(particle);
            getChildren().add(particle.shape);
        }
        resizeParticles();
    }

    void setWindowActive(boolean active) {
        if (active && !particles.isEmpty()) startAnimation();
        else stopAnimation();
    }

    void dispose() {
        stopAnimation();
        particles.clear();
        getChildren().clear();
    }

    private static boolean isAnimatedTheme(ThemeManager.Theme theme) {
        return theme == ThemeManager.Theme.PINK_BLOSSOM || theme == ThemeManager.Theme.NATURE_BREEZE;
    }

    private Particle createParticle(int index) {
        boolean left = index % 2 == 0;
        Shape shape;
        Color color;
        if (theme == ThemeManager.Theme.PINK_BLOSSOM) {
            shape = new Ellipse(0, 0, 4 + RANDOM.nextDouble() * 3, 6 + RANDOM.nextDouble() * 4);
            Color[] colors = {Color.web("#F9A8D4"), Color.web("#FBCFE8"), Color.web("#FCE7F3"), Color.web("#EC4899")};
            color = colors[RANDOM.nextInt(colors.length)];
        } else {
            shape = makeLeaf();
            Color[] colors = {Color.web("#66BB6A"), Color.web("#81C784"), Color.web("#A5D6A7"), Color.web("#D6A756")};
            color = colors[RANDOM.nextInt(colors.length)];
        }
        shape.setFill(color);
        shape.setOpacity(0.36 + RANDOM.nextDouble() * 0.34);
        shape.setRotate(RANDOM.nextDouble() * 360);
        shape.setScaleX(0.65 + RANDOM.nextDouble() * 0.65);
        shape.setScaleY(0.65 + RANDOM.nextDouble() * 0.65);
        shape.setManaged(false);
        shape.setMouseTransparent(true);

        double slow = 15 + RANDOM.nextDouble() * 15;
        double normal = 25 + RANDOM.nextDouble() * 22;
        double velocity = speed == ThemeManager.AmbientSpeed.SLOW ? slow : normal;
        double amplitude = 4 + RANDOM.nextDouble() * 11;
        double angularVelocity = (RANDOM.nextDouble() * 60 - 30) * (speed == ThemeManager.AmbientSpeed.SLOW ? 0.6 : 1.0);
        return new Particle(shape, left, velocity, amplitude, angularVelocity, RANDOM.nextDouble() * Math.PI * 2);
    }

    private static Path makeLeaf() {
        Path leaf = new Path(
                new MoveTo(0, 0),
                new QuadCurveTo(18, 2, 19, 0),
                new QuadCurveTo(15, 14, 0, 18),
                new QuadCurveTo(5, 8, 0, 0),
                new ClosePath());
        leaf.setStroke(Color.color(0.18, 0.42, 0.2, 0.34));
        leaf.setStrokeWidth(0.55);
        return leaf;
    }

    private void resizeParticles() {
        Rectangle clip = (Rectangle) getClip();
        clip.setWidth(Math.max(0, getWidth()));
        clip.setHeight(Math.max(0, getHeight()));
        if (particles.isEmpty() || getWidth() <= 0 || getHeight() <= 0) return;
        double band = Math.max(28, Math.min(82, getWidth() * 0.085));
        for (Particle particle : particles) {
            double offset = RANDOM.nextDouble() * band;
            particle.baseX = particle.left ? offset : getWidth() - band + offset;
            particle.y = RANDOM.nextDouble() * getHeight();
            particle.shape.setLayoutX(particle.baseX);
            particle.shape.setLayoutY(particle.y);
        }
    }

    private void startAnimation() {
        if (running || particles.isEmpty()) return;
        lastFrame = 0;
        running = true;
        timer.start();
    }

    private void stopAnimation() {
        if (!running) return;
        running = false;
        timer.stop();
        lastFrame = 0;
    }

    private void animate(long now) {
        if (lastFrame == 0) {
            lastFrame = now;
            return;
        }
        double delta = Math.min(0.05, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        double height = getHeight();
        double width = getWidth();
        if (height <= 0 || width <= 0) return;
        for (Particle particle : particles) {
            particle.y += particle.velocity * delta;
            particle.phase += delta * (speed == ThemeManager.AmbientSpeed.SLOW ? 0.75 : 1.15);
            if (particle.y > height + 24) {
                particle.y = -20 - RANDOM.nextDouble() * 80;
                double band = Math.max(28, Math.min(82, width * 0.085));
                double offset = RANDOM.nextDouble() * band;
                particle.baseX = particle.left ? offset : width - band + offset;
            }
            particle.shape.setLayoutX(particle.baseX + Math.sin(particle.phase) * particle.amplitude);
            particle.shape.setLayoutY(particle.y);
            particle.shape.setRotate(particle.shape.getRotate() + particle.rotationVelocity * delta);
        }
        if (delta > 0.045) slowFrames++;
        else slowFrames = Math.max(0, slowFrames - 1);
        if (slowFrames > 120 && particles.size() > 3) {
            int reducedCount = Math.max(3, (int) Math.ceil(particles.size() * 0.72));
            while (particles.size() > reducedCount) {
                Particle removed = particles.remove(particles.size() - 1);
                getChildren().remove(removed.shape);
            }
            slowFrames = 0;
        }
    }

    private static final class Particle {
        final Shape shape;
        final boolean left;
        final double velocity;
        final double amplitude;
        final double rotationVelocity;
        double phase;
        double baseX;
        double y;

        Particle(Shape shape, boolean left, double velocity, double amplitude,
                 double rotationVelocity, double phase) {
            this.shape = shape;
            this.left = left;
            this.velocity = velocity;
            this.amplitude = amplitude;
            this.rotationVelocity = rotationVelocity;
            this.phase = phase;
        }
    }
}
