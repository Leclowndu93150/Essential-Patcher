package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.compat.IrisCompat;
import com.leclowndu93150.essentialpatcher.compat.ShaderCompat;
import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.model.ParticleSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleSystem.class, remap = false)
public class ParticleSystemMixin {

    @Unique
    private int[] essentialPatcher$capturedStateSnapshot;

    @Inject(method = "render(Lgg/essential/model/util/UMatrixStack;Lgg/essential/lib/kotgl/matrix/vectors/Vec3;Lgg/essential/model/util/Quaternion;Lgg/essential/model/backend/RenderBackend$CommandQueue;Ljava/util/UUID;ZZLjava/util/UUID;)V", at = @At("HEAD"))
    private void essentialPatcher$isolateCapturedStateStart(CallbackInfo ci) {
        if (!PatcherConfig.get().fixShaderGlintCorruption) return;
        if (!ShaderCompat.isShaderPackActive()) return;
        if (!IrisCompat.isAvailable()) return;

        essentialPatcher$capturedStateSnapshot = IrisCompat.snapshotCapturedState();
        IrisCompat.restoreCapturedState(new int[]{0, 0, 0});
    }

    @Inject(method = "render(Lgg/essential/model/util/UMatrixStack;Lgg/essential/lib/kotgl/matrix/vectors/Vec3;Lgg/essential/model/util/Quaternion;Lgg/essential/model/backend/RenderBackend$CommandQueue;Ljava/util/UUID;ZZLjava/util/UUID;)V", at = @At("RETURN"))
    private void essentialPatcher$isolateCapturedStateEnd(CallbackInfo ci) {
        if (essentialPatcher$capturedStateSnapshot == null) return;
        IrisCompat.restoreCapturedState(essentialPatcher$capturedStateSnapshot);
        essentialPatcher$capturedStateSnapshot = null;
    }
}
