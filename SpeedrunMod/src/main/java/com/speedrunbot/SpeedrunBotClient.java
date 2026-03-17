package com.speedrunbot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Point d'entree du mod (cote client uniquement).
 *
 * Enregistre:
 *  - La touche B pour ouvrir/fermer le GUI du bot
 *  - Le tick event pour appeler BotEngine.tick() chaque tick
 */
public class SpeedrunBotClient implements ClientModInitializer {

    private KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        // Touche B (GLFW_KEY_B = 66) pour ouvrir le GUI
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.speedrunbot.opengui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "SpeedrunBot"
        ));

        // Tick event : appelé 20 fois/seconde
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Ouvre le GUI si la touche est pressee
            if (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new BotScreen());
                } else if (client.currentScreen instanceof BotScreen) {
                    client.setScreen(null);
                }
            }

            // Appelle le moteur du bot
            BotEngine.INSTANCE.tick(client);
        });

        System.out.println("[SpeedrunBot] Mod charge ! Appuie sur B en jeu pour ouvrir le GUI.");
    }
}
