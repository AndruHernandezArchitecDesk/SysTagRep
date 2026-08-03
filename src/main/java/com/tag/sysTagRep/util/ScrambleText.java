package com.tag.sysTagRep.util;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;

public class ScrambleText {

    private static final String GLYPHS = "!<>-_\\/[]{}—=+*^?#|:;,.ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final double REPEAT_DELAY_SECONDS = 1.2;

    private final Text textNode;
    private final Label labelNode;
    private final String finalText;
    private final long staggerNanos;
    private final long scrambleNanos;
    private boolean repeat;

    public ScrambleText(Text node, String finalText) {
        this(node, finalText, 45_000_000L, 350_000_000L);
    }

    public ScrambleText(Text node, String finalText, long staggerNanos, long scrambleNanos) {
        this.textNode = node;
        this.labelNode = null;
        this.finalText = finalText;
        this.staggerNanos = staggerNanos;
        this.scrambleNanos = scrambleNanos;
        node.setFont(Font.font("Monospaced", FontWeight.BOLD, node.getFont().getSize()));
        node.setAccessibleText(finalText);
    }

    public ScrambleText(Label node, String finalText) {
        this(node, finalText, 45_000_000L, 350_000_000L);
    }

    public ScrambleText(Label node, String finalText, long staggerNanos, long scrambleNanos) {
        this.textNode = null;
        this.labelNode = node;
        this.finalText = finalText;
        this.staggerNanos = staggerNanos;
        this.scrambleNanos = scrambleNanos;
        node.setFont(Font.font("Monospaced", FontWeight.BOLD, node.getFont().getSize()));
        node.setAccessibleText(finalText);
    }

    public ScrambleText repeat(boolean repeat) {
        this.repeat = repeat;
        return this;
    }

    public void play() {
        if (isReduceMotion()) {
            setText(finalText);
            return;
        }
        startCycle();
    }

    private void startCycle() {
        long[] settleAt = new long[finalText.length()];
        for (int i = 0; i < finalText.length(); i++) {
            settleAt[i] = i * staggerNanos + scrambleNanos;
        }

        new AnimationTimer() {
            private Long startNanos;

            @Override
            public void handle(long now) {
                if (startNanos == null) startNanos = now;
                long elapsed = now - startNanos;

                StringBuilder sb = new StringBuilder(finalText.length());
                boolean done = true;
                for (int i = 0; i < finalText.length(); i++) {
                    char c = finalText.charAt(i);
                    if (Character.isWhitespace(c) || elapsed >= settleAt[i]) {
                        sb.append(c);
                    } else {
                        done = false;
                        sb.append(randomGlyph());
                    }
                }

                setText(sb.toString());
                if (done) {
                    setText(finalText);
                    stop();
                    if (repeat) {
                        PauseTransition pause = new PauseTransition(Duration.seconds(REPEAT_DELAY_SECONDS));
                        pause.setOnFinished(e -> startCycle());
                        pause.play();
                    }
                }
            }
        }.start();
    }

    private void setText(String value) {
        if (textNode != null) {
            textNode.setText(value);
        } else {
            labelNode.setText(value);
        }
    }

    private static char randomGlyph() {
        return GLYPHS.charAt(ThreadLocalRandom.current().nextInt(GLYPHS.length()));
    }

    private static boolean isReduceMotion() {
        String v = System.getProperty("systagrep.reduceMotion");
        if (v == null) return false;
        return v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes");
    }
}
