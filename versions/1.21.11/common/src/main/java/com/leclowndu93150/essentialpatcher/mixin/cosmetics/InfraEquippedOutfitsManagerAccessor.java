package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import gg.essential.network.connectionmanager.cosmetics.InfraEquippedOutfitsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(value = InfraEquippedOutfitsManager.class, remap = false)
public interface InfraEquippedOutfitsManagerAccessor {

    @Accessor("infraOutfits")
    Map<UUID, InfraEquippedOutfitsManager.InfraOutfit> essentialPatcher$getInfraOutfits();
}
