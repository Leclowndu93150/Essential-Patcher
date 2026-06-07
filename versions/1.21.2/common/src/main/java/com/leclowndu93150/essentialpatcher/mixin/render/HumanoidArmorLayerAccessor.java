package com.leclowndu93150.essentialpatcher.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAccessor {

    @Invoker("renderArmorPiece")
    void essentialPatcher$invokeRenderArmorPiece(PoseStack pose, MultiBufferSource buffer, ItemStack stack,
                                                 EquipmentSlot slot, int packedLight, HumanoidModel<HumanoidRenderState> model);

    @Invoker("getArmorModel")
    HumanoidModel<HumanoidRenderState> essentialPatcher$invokeGetArmorModel(HumanoidRenderState state, EquipmentSlot slot);
}
