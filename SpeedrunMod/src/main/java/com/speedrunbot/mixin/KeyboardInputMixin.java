package com.speedrunbot.mixin;

import com.speedrunbot.BotEngine;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * S'injecte APRES que Minecraft a lu le clavier.
 * On ecrase les valeurs de mouvement avec celles du bot.
 * C'est la methode la plus fiable — on joue directement dans
 * les variables que Minecraft utilise pour deplacer le joueur.
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(boolean slowDown, float f, CallbackInfo ci) {
        BotEngine bot = BotEngine.INSTANCE;
        if (!bot.isActive()) return;

        // Cast: KeyboardInput etend Input, donc (Input)(Object)this fonctionne
        Input self = (Input)(Object)this;

        // Ecrase les inputs clavier par les valeurs du bot
        self.movementForward  = bot.moveForward;
        self.movementSideways = bot.moveSideways;
        self.jumping          = bot.jumping;
        self.sneaking         = bot.sneaking;
    }
}
