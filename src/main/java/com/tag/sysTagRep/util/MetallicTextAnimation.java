package com.tag.sysTagRep.util;

import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MetallicTextAnimation {

    public static class Config {
        public String text;
        public double fontSize = 26;
        public double staggerMs = 55;
        public double assemblyMs = 260;
        public int fragmentsPerChar = 8;
        public double spread = 70;
        public Color flashColor = Color.web("#a5dcff");
        public double sweepDurationMs = 380;
        public double vibrationDurationMs = 150;
        public double energyDurationMs = 420;
        public List<Color> metallicColors = new ArrayList<>(List.of(
                Color.web("#3a3f45"),
                Color.web("#8a9098"),
                Color.web("#d6dae0"),
                Color.web("#eef1f4")));

        public Config(String text) {
            this.text = text;
        }
    }

    private static class Fragment {
        final Rectangle shape;
        final double ax, ay, bx, by, cx, cy, startRot;

        Fragment(Rectangle shape, double ax, double ay, double bx, double by, double cx, double cy, double startRot) {
            this.shape = shape;
            this.ax = ax;
            this.ay = ay;
            this.bx = bx;
            this.by = by;
            this.cx = cx;
            this.cy = cy;
            this.startRot = startRot;
        }
    }

    private static class CharData {
        final Group group = new Group();
        final Text glyph;
        final double startMs;
        final List<Fragment> fragments = new ArrayList<>();

        CharData(Text glyph, double startMs) {
            this.glyph = glyph;
            this.startMs = startMs;
        }
    }

    private final Config cfg;
    private final List<CharData> chars = new ArrayList<>();
    private AnimationTimer timer;
    private SequentialTransition tail;
    private Group titleGroup;
    private Rectangle sweep;
    private Circle energy;
    private double totalW;

    public MetallicTextAnimation(String text) {
        this(new Config(text));
    }

    public MetallicTextAnimation(Config cfg) {
        this.cfg = cfg;
    }

    public void stop() {
        if (timer != null) timer.stop();
        if (tail != null) tail.stop();
    }

    public Group play(Pane parent) {
        if (isReduceMotion()) return showStatic(parent);
        build(parent);
        animate();
        return (Group) parent.getChildren().get(parent.getChildren().size() - 1);
    }

    private static Text styledText(String value, Font font) {
        Text t = new Text(value);
        t.setFont(font);
        return t;
    }

    private Group showStatic(Pane parent) {
        Font font = Font.font("System", FontWeight.BOLD, cfg.fontSize);
        Text t = styledText(cfg.text, font);
        t.setFill(metallicGradient());
        t.setEffect(null);

        double pad = 30;
        double w = t.getLayoutBounds().getWidth() + pad * 2;
        double h = t.getLayoutBounds().getHeight() + pad * 2;
        parent.setPrefSize(w, h);
        t.setTranslateX(pad);
        t.setTranslateY(pad + Math.max(0, -t.getLayoutBounds().getMinY()));

        Group root = new Group(t);
        parent.getChildren().add(root);
        return root;
    }

    private void build(Pane parent) {
        Font font = Font.font("System", FontWeight.BOLD, cfg.fontSize);
        Text probe = styledText("Mg", font);
        double ascent = -probe.getLayoutBounds().getMinY();
        double lineHeight = probe.getLayoutBounds().getHeight();

        List<Double> widths = new ArrayList<>();
        totalW = 0;
        for (int i = 0; i < cfg.text.length(); i++) {
            char c = cfg.text.charAt(i);
            double cw = styledText(String.valueOf(c), font).getLayoutBounds().getWidth();
            widths.add(cw);
            totalW += (c == ' ') ? cw * 0.6 : cw;
        }

        double titleHeight = lineHeight + 14;
        double x = 0;
        titleGroup = new Group();
        for (int i = 0; i < cfg.text.length(); i++) {
            char c = cfg.text.charAt(i);
            double cw = widths.get(i);
            if (c == ' ') {
                x += cw * 0.6;
                continue;
            }

            double cx = cw / 2.0;
            double cy = ascent * 0.55;

            Text glyph = styledText(String.valueOf(c), font);
            glyph.setX(0);
            glyph.setY(ascent);
            glyph.setFill(metallicGradient());
            glyph.setOpacity(0);

            CharData cd = new CharData(glyph, i * cfg.staggerMs);
            cd.group.getChildren().add(glyph);

            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int f = 0; f < cfg.fragmentsPerChar; f++) {
                double fw = rnd.nextDouble(2.0, 5.5);
                double fh = rnd.nextDouble(2.0, 8.5);
                Rectangle rect = new Rectangle(fw, fh);
                rect.setArcWidth(1);
                rect.setArcHeight(1);
                rect.setFill(cfg.metallicColors.get(rnd.nextInt(cfg.metallicColors.size())));
                rect.setEffect(new GaussianBlur(0.8));

                double ang = rnd.nextDouble(0, 2 * Math.PI);
                double dist = rnd.nextDouble(cfg.spread * 0.6, cfg.spread);
                double ax = cx + Math.cos(ang) * dist;
                double ay = cy + Math.sin(ang) * dist;
                double bendDir = rnd.nextBoolean() ? 1 : -1;
                double bx = (ax + cx) / 2 + Math.cos(ang + Math.PI / 2) * cfg.spread * 0.35 * bendDir;
                double by = (ay + cy) / 2 + Math.sin(ang + Math.PI / 2) * cfg.spread * 0.35 * bendDir;

                rect.setTranslateX(ax);
                rect.setTranslateY(ay);
                double rot = rnd.nextDouble(-170, 170);
                rect.setRotate(rot);
                rect.setOpacity(0.95);
                cd.fragments.add(new Fragment(rect, ax, ay, bx, by, cx, cy, rot));
                cd.group.getChildren().add(rect);
            }

            cd.group.setTranslateX(x);
            titleGroup.getChildren().add(cd.group);
            chars.add(cd);
            x += cw;
        }

        energy = new Circle(0, 0, 1,
                new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                        new Stop(0, cfg.flashColor.deriveColor(0, 1, 1, 0.9)),
                        new Stop(1, cfg.flashColor.deriveColor(0, 1, 1, 0))));
        energy.setOpacity(0);

        double sweepW = Math.max(totalW * 0.35, 90);
        sweep = new Rectangle(sweepW, titleHeight);
        sweep.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.5, Color.WHITE.deriveColor(0, 1, 1, 0.85)),
                new Stop(1, Color.TRANSPARENT)));
        sweep.setBlendMode(BlendMode.SCREEN);
        sweep.setTranslateX(-sweepW);
        sweep.setVisible(false);
        titleGroup.getChildren().add(sweep);

        titleGroup.setClip(new Rectangle(0, 0, totalW + 4, titleHeight));

        double pad = 30;
        parent.setPrefSize(totalW + pad * 2, titleHeight + pad * 2);
        titleGroup.setTranslateX(pad);
        titleGroup.setTranslateY(pad);
        energy.setTranslateX(pad + totalW / 2);
        energy.setTranslateY(pad + titleHeight / 2);

        Group root = new Group(energy, titleGroup);
        parent.getChildren().add(root);
    }

    private void animate() {
        long[] start = new long[]{0};
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (start[0] == 0) start[0] = now;
                double ms = (now - start[0]) / 1_000_000.0;

                double ep = clamp(ms / cfg.energyDurationMs, 0, 1);
                energy.setRadius(6 + ep * cfg.spread * 2.2);
                energy.setOpacity((1 - ep) * 0.55);

                double lastDone = 0;
                for (CharData cd : chars) {
                    double p = clamp((ms - cd.startMs) / cfg.assemblyMs, 0, 1);
                    lastDone = Math.max(lastDone, cd.startMs + cfg.assemblyMs);

                    double e = easeInOutCubic(p);
                    for (Fragment f : cd.fragments) {
                        f.shape.setTranslateX(bezier(f.ax, f.bx, f.cx, e));
                        f.shape.setTranslateY(bezier(f.ay, f.by, f.cy, e));
                        f.shape.setRotate(f.startRot * (1 - e));
                        f.shape.setOpacity(0.95 * (1 - e));
                    }

                    double appear = smoothstep(0.7, 1.0, p);
                    cd.glyph.setOpacity(appear);
                }

                if (ms >= lastDone) {
                    stop();
                    launchTail();
                }
            }
        };
        timer.start();
    }

    private void launchTail() {
        double sweepW = sweep.getWidth();
        sweep.setVisible(true);

        TranslateTransition sweepAnim = new TranslateTransition(Duration.millis(cfg.sweepDurationMs), sweep);
        sweepAnim.setFromX(-sweepW);
        sweepAnim.setToX(totalW + sweepW);
        sweepAnim.setInterpolator(Interpolator.EASE_BOTH);

        Timeline vib = new Timeline(
                new KeyFrame(Duration.ZERO, e -> titleGroup.setTranslateY(1.5)),
                new KeyFrame(Duration.millis(35), e -> titleGroup.setTranslateY(-1.5)),
                new KeyFrame(Duration.millis(70), e -> titleGroup.setTranslateY(1.0)),
                new KeyFrame(Duration.millis(105), e -> titleGroup.setTranslateY(-1.0)),
                new KeyFrame(Duration.millis(cfg.vibrationDurationMs), e -> titleGroup.setTranslateY(0)));

        tail = new SequentialTransition(sweepAnim, vib);
        tail.play();
    }

    private LinearGradient metallicGradient() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, cfg.metallicColors.get(0)),
                new Stop(0.45, cfg.metallicColors.get(1)),
                new Stop(0.75, cfg.metallicColors.get(2)),
                new Stop(1, cfg.metallicColors.get(3)));
    }

    private static double bezier(double p0, double p1, double p2, double t) {
        double u = 1 - t;
        return u * u * p0 + 2 * u * t * p1 + t * t * p2;
    }

    private static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private static double smoothstep(double e0, double e1, double x) {
        double t = clamp((x - e0) / (e1 - e0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    private static boolean isReduceMotion() {
        String v = System.getProperty("systagrep.reduceMotion");
        if (v == null) return false;
        return v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes");
    }
}
