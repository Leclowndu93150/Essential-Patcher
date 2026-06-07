package com.leclowndu93150.essentialpatcher.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAccessor {

    @Invoker("renderArmorPiece")
    void essentialPatcher$invokeRenderArmorPiece(PoseStack pose, MultiBufferSource buffer, LivingEntity entity,
                                                 EquipmentSlot slot, int packedLight, HumanoidModel<LivingEntity> model);

    @Invoker("getArmorModel")
    HumanoidModel<LivingEntity> essentialPatcher$invokeGetArmorModel(EquipmentSlot slot);
}
