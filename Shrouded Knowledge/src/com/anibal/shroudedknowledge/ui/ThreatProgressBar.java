package com.anibal.shroudedknowledge.ui;

import java.awt.Color;
import java.util.List;

import com.anibal.shroudedknowledge.ShroudedKnowledgeManager;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

public class ThreatProgressBar implements CustomUIPanelPlugin {

    private static final int MAX_POINTS = 300;

    private static final float BAR_HEIGHT = 14f;
    private static final float MARKER_WIDTH = 2f;
    private static final float MARKER_HEIGHT = 18f;

    private static final Color BACKGROUND_COLOR =
            new Color(35, 35, 42);

    private static final Color PROGRESS_COLOR =
            new Color(170, 35, 45);

    private static final Color FIRST_MARKER_COLOR =
            new Color(210, 165, 70);

    private static final Color FINAL_MARKER_COLOR =
            new Color(220, 65, 65);

    private final SpriteAPI sprite;

    private PositionAPI position;

    public ThreatProgressBar() {
        sprite = Global.getSettings().getSprite(
                "shrouded_knowledge",
                "progress_bar"
        );
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        // No necesitamos dibujar nada detrás del panel.
    }

    @Override
    public void render(float alphaMult) {

        if (position == null) {
            return;
        }

        float x = position.getX();
        float y = position.getY();

        float width = position.getWidth();
        float height = position.getHeight();

        if (width <= 0f || height <= 0f) {
            return;
        }

        // Obtener los puntos actuales de amenaza.
        ShroudedKnowledgeManager manager =
                ShroudedKnowledgeManager.getInstance();

        int threatPoints = manager != null
                ? manager.getThreatPoints()
                : 0;

        // Limitar los puntos entre 0 y 300.
        threatPoints = Math.max(
                0,
                Math.min(MAX_POINTS, threatPoints)
        );

        // Calcular el progreso.
        float progress = threatPoints / (float) MAX_POINTS;

        float progressWidth = width * progress;

        float centerY = y + height / 2f;

        // 1. Fondo de la barra.
        drawRect(
                x + width / 2f,
                centerY,
                width,
                BAR_HEIGHT,
                BACKGROUND_COLOR,
                alphaMult
        );

        // 2. Progreso rojo.
        if (progressWidth > 0f) {
            drawRect(
                    x + progressWidth / 2f,
                    centerY,
                    progressWidth,
                    BAR_HEIGHT,
                    PROGRESS_COLOR,
                    alphaMult
            );
        }

        // 3. Marca del primer hito: 150 puntos.
        drawRect(
                x + width * 0.5f,
                centerY,
                MARKER_WIDTH,
                MARKER_HEIGHT,
                FIRST_MARKER_COLOR,
                alphaMult
        );

        // 4. Marca del hito final: 300 puntos.
        drawRect(
                x + width - MARKER_WIDTH / 2f,
                centerY,
                MARKER_WIDTH,
                MARKER_HEIGHT,
                FINAL_MARKER_COLOR,
                alphaMult
        );
    }

    private void drawRect(
            float centerX,
            float centerY,
            float width,
            float height,
            Color color,
            float alphaMult) {

        sprite.setSize(width, height);
        sprite.setColor(color);
        sprite.setAlphaMult(alphaMult);
        sprite.renderAtCenter(centerX, centerY);
    }

    @Override
    public void advance(float amount) {
        // La barra consulta los puntos al dibujarse.
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        // La barra no necesita interacción.
    }

    @Override
    public void buttonPressed(Object buttonId) {
        // No utilizamos botones.
    }
}
