package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.compat.IrisCompat;
import com.leclowndu93150.essentialpatcher.compat.ShaderCompat;
import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import gg.essential.model.backend.minecraft.MinecraftRenderBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftRenderBackend.ParticleVertexConsumerProvider.class, remap = false)
public class ParticleVertexConsumerProviderMixin {

    @Unique
    private int essentialPatcher$previousShaderTexture = -1;

    @Unique
    private int[] essentialPatcher$capturedStateSnapshot;

    @Inject(method = "provide", at = @At("HEAD"))
    private void essentialPatcher$saveRenderState(CallbackInfo ci) {
        if (!PatcherConfig.get().fixShaderGlintCorruption) return;

        essentialPatcher$previousShaderTexture = RenderSystem.getShaderTexture(0);
        if (ShaderCompat.isShaderPackActive() && IrisCompat.isAvailable()) {
            essentialPatcher$capturedStateSnapshot = IrisCompat.snapshotCapturedState();
            IrisCompat.restoreCapturedState(new int[]{0, 0, 0});
        }
    }

    @Inject(method = "provide", at = @At("RETURN"))
    private void essentialPatcher$restoreRenderState(CallbackInfo ci) {
        if (essentialPatcher$previousShaderTexture != -1) {
            RenderSystem.setShaderTexture(0, essentialPatcher$previousShaderTexture);
            essentialPatcher$previousShaderTexture = -1;
        }
        if (essentialPatcher$capturedStateSnapshot != null) {
            IrisCompat.restoreCapturedState(essentialPatcher$capturedStateSnapshot);
            essentialPatcher$capturedStateSnapshot = null;
        }
    }
}
