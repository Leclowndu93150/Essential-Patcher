package com.leclowndu93150.essentialpatcher.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void essentialPatcher$gateArmorRender(PoseStack pose, MultiBufferSource buffer, int packedLight,
                                                  HumanoidRenderState state, float yRot, float xRot,
                                                  CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderStateExt ext)) return;

        Set<Integer> blocked;
        try {
            CosmeticsRenderState rs = ext.essential$getCosmetics();
            if (rs == null) return;
            blocked = rs.blockedArmorSlots();
        } catch (Throwable t) {
            return;
        }
        if (blocked.isEmpty()) return;

        ci.cancel();

        HumanoidArmorLayerAccessor accessor = (HumanoidArmorLayerAccessor) this;
        EquipmentSlot[] slots = { EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD };
        ItemStack[] stacks = { state.chestItem, state.legsItem, state.feetItem, state.headItem };
        for (int i = 0; i < slots.length; i++) {
            EquipmentSlot slot = slots[i];
            if (blocked.contains(slot.getIndex())) continue;
            HumanoidModel<HumanoidRenderState> model = accessor.essentialPatcher$invokeGetArmorModel(state, slot);
            accessor.essentialPatcher$invokeRenderArmorPiece(pose, buffer, stacks[i], slot, packedLight, model);
        }
    }
}
