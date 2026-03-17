package com.speedrunbot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Ecran GUI du bot, accessible avec la touche B en jeu.
 * Style sombre, panneau de logs, boutons Start/Stop.
 */
public class BotScreen extends Screen {

    private static final int BG      = 0xDD0a0a0f;
    private static final int PANEL   = 0xCC12121a;
    private static final int ACCENT  = 0xFF4a9eff;
    private static final int GREEN   = 0xFF00ff88;
    private static final int RED     = 0xFFff4455;
    private static final int YELLOW  = 0xFFffcc00;
    private static final int WHITE   = 0xFFe8e8ff;
    private static final int GREY    = 0xFF666680;

    private static final List<String> LOGS = new ArrayList<>();
    private static final int MAX_LOGS = 18;

    private String mode = "normal";  // "normal" ou "speedrun"

    private ButtonWidget btnStart;
    private ButtonWidget btnStop;
    private ButtonWidget btnModeN;
    private ButtonWidget btnModeS;

    public BotScreen() {
        super(Text.literal("SpeedrunBot"));
    }

    public static void addLog(String msg) {
        LOGS.add(msg);
        while (LOGS.size() > MAX_LOGS) LOGS.remove(0);
    }

    @Override
    protected void init() {
        int w = this.width;
        int h = this.height;
        int panelW = 280;
        int panelX = (w - panelW) / 2;

        // Boutons mode
        btnModeN = ButtonWidget.builder(Text.literal("🌿 NORMAL"), btn -> selectMode("normal"))
                .dimensions(panelX, h / 2 - 75, 135, 22)
                .build();
        btnModeS = ButtonWidget.builder(Text.literal("⚡ SPEEDRUN"), btn -> selectMode("speedrun"))
                .dimensions(panelX + 140, h / 2 - 75, 140, 22)
                .build();

        // Boutons start / stop
        btnStart = ButtonWidget.builder(Text.literal("▶  DÉMARRER"), btn -> startBot())
                .dimensions(panelX, h / 2 + 105, 135, 24)
                .build();
        btnStop = ButtonWidget.builder(Text.literal("⏹  ARRÊTER"), btn -> stopBot())
                .dimensions(panelX + 140, h / 2 + 105, 140, 24)
                .build();

        addDrawableChild(btnModeN);
        addDrawableChild(btnModeS);
        addDrawableChild(btnStart);
        addDrawableChild(btnStop);

        btnStop.active = false;
        selectMode("normal");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Fond semi-transparent
        ctx.fill(0, 0, this.width, this.height, 0xBB000000);

        int w       = this.width;
        int h       = this.height;
        int panelW  = 280;
        int panelH  = 270;
        int panelX  = (w - panelW) / 2;
        int panelY  = h / 2 - panelH / 2;

        // Panneau principal
        ctx.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, ACCENT);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF0a0a0f);

        // Titre
        ctx.drawCenteredTextWithShadow(textRenderer, "⚡ SPEEDRUN BOT", w / 2, panelY + 8, ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer, "Minecraft 1.21  •  Tue l'Ender Dragon", w / 2, panelY + 20, GREY);

        // Ligne separatrice
        ctx.fill(panelX + 5, panelY + 32, panelX + panelW - 5, panelY + 33, ACCENT);

        // Label mode
        ctx.drawTextWithShadow(textRenderer, "MODE :", panelX + 5, panelY + 40, WHITE);

        // Phase courante
        String phaseStr = BotEngine.INSTANCE.isActive()
                ? "Phase: " + BotEngine.INSTANCE.getPhase().name()
                : "Inactif — pret a demarrer";
        int phaseColor = BotEngine.INSTANCE.isActive() ? GREEN : GREY;
        ctx.drawCenteredTextWithShadow(textRenderer, phaseStr, w / 2, panelY + 63, phaseColor);

        // Ligne separatrice
        ctx.fill(panelX + 5, panelY + 75, panelX + panelW - 5, panelY + 76, 0xFF1a1a28);

        // Zone de logs
        int logY = panelY + 80;
        ctx.fill(panelX + 4, logY - 2, panelX + panelW - 4, logY + MAX_LOGS * 9 + 2, 0xCC050508);
        ctx.drawTextWithShadow(textRenderer, "CONSOLE :", panelX + 6, logY - 10, GREY);

        for (int i = 0; i < LOGS.size(); i++) {
            String line = LOGS.get(i);
            int color = WHITE;
            if (line.contains("[PHASE]") || line.contains("[BOT]")) color = ACCENT;
            else if (line.contains("DRAGON") || line.contains("VICTOIRE") || line.contains("TERMINE")) color = YELLOW;
            else if (line.contains("ARRETE") || line.contains("ERREUR")) color = RED;
            else if (line.contains("OK") || line.contains("coupe") || line.contains("collecte")) color = GREEN;
            ctx.drawTextWithShadow(textRenderer, line.length() > 46 ? line.substring(0, 46) : line,
                    panelX + 6, logY + i * 9, color);
        }

        // Touche pour fermer
        ctx.drawCenteredTextWithShadow(textRenderer, "Echap / B pour fermer", w / 2, panelY + panelH - 12, GREY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void selectMode(String m) {
        this.mode = m;
        // Met en evidence le bouton selectionne
        // (les ButtonWidget Vanilla n'ont pas de setHighlighted direct,
        //  on change le message pour indiquer la selection)
        btnModeN.setMessage(Text.literal("normal".equals(m)  ? "✓ NORMAL"   : "  NORMAL"));
        btnModeS.setMessage(Text.literal("speedrun".equals(m) ? "✓ SPEEDRUN" : "  SPEEDRUN"));
    }

    private void startBot() {
        if (BotEngine.INSTANCE.isActive()) return;
        BotEngine.INSTANCE.start(mode,
                msg -> { addLog(msg); },
                phase -> {}
        );
        btnStart.active = false;
        btnStop.active  = true;
        // Ferme le GUI — le bot joue pendant qu'on est in-game
        this.client.setScreen(null);
    }

    private void stopBot() {
        BotEngine.INSTANCE.stop();
        btnStart.active = true;
        btnStop.active  = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // B ou Echap ferme le GUI
        if (keyCode == 66 || keyCode == 256) {  // B = 66, Echap = 256
            this.client.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        // Le jeu NE se met pas en pause quand ce GUI est ouvert
        return false;
    }
}
