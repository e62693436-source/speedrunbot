package com.speedrunbot.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Expose la methode setPressed de KeyBinding
 * (normalement package-private ou inexistante en public).
 * Fabric recommande cette approche Accessor pour acceder
 * aux champs internes sans reflection.
 */
@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("pressed")
    void setPressed(boolean pressed);
}
