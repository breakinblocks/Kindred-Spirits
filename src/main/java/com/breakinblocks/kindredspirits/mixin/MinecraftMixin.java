package com.breakinblocks.kindredspirits.mixin;

import com.breakinblocks.kindredspirits.client.PackGlowOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void kindredspirits$packGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (PackGlowOverlay.isMarked(entity)) {
            cir.setReturnValue(true);
        }
    }
}
