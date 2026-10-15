package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.compat.ShaderCompat;
import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.cosmetics.EssentialModelRenderer;
import gg.essential.mixins.impl.client.gui.GuiInventoryExt;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EssentialModelRenderer.Companion.class, remap = false)
public class EssentialModelRendererCompanionMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void essentialPatcher$hideInInventoryWithShaders(AbstractClientPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!PatcherConfig.get().hideCosmeticsInInventoryWithShaders) return;
        if (!GuiInventoryExt.isInventoryEntityRendering.getUntracked()) return;
        if (!ShaderCompat.isShaderPackActive()) return;
        cir.setReturnValue(false);
    }
}
