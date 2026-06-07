package com.leclowndu93150.essentialpatcher.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.essential.cosmetics.CosmeticsRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void essentialPatcher$gateArmorRender(PoseStack pose, MultiBufferSource buffer, int packedLight,
                                                  LivingEntity entity, float f1, float f2, float f3, float f4,
                                                  float f5, float f6, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;

        Set<Integer> blocked;
        try {
            blocked = new CosmeticsRenderState.Live(player).blockedArmorSlots();
        } catch (Throwable t) {
            return;
        }
        if (blocked.isEmpty()) return;

        ci.cancel();

        HumanoidArmorLayerAccessor accessor = (HumanoidArmorLayerAccessor) this;
        EquipmentSlot[] slots = { EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD };
        for (EquipmentSlot slot : slots) {
            if (blocked.contains(slot.getIndex())) continue;
            HumanoidModel<LivingEntity> model = accessor.essentialPatcher$invokeGetArmorModel(slot);
            accessor.essentialPatcher$invokeRenderArmorPiece(pose, buffer, entity, slot, packedLight, model);
        }
    }
}
