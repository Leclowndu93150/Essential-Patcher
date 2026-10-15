package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.compat.ShaderCompat;
import com.leclowndu93150.essentialpatcher.config.PatcherConfig;
import gg.essential.api.cosmetics.RenderCosmetic;
import gg.essential.config.EssentialConfig;
import gg.essential.cosmetics.ArmorSlots;
import gg.essential.cosmetics.CosmeticsRenderState;
import gg.essential.cosmetics.CosmeticsState;
import gg.essential.cosmetics.EquippedCosmetic;
import gg.essential.cosmetics.WearablesManager;
import gg.essential.mixins.impl.client.entity.AbstractClientPlayerExt;
import gg.essential.mixins.impl.client.gui.GuiInventoryExt;
import gg.essential.mod.cosmetics.settings.CosmeticProperty;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(value = CosmeticsRenderState.Live.class, remap = false)
public abstract class CosmeticsRenderStateLiveMixin {

    @Unique
    private static final Method essentialPatcher$aetherGlovesMethod = essentialPatcher$findAetherGlovesMethod();

    @Shadow
    @Final
    private AbstractClientPlayer player;

    @Inject(method = "wearablesManager", at = @At("HEAD"), cancellable = true)
    private void essentialPatcher$hideInventoryCosmetics(CallbackInfoReturnable<WearablesManager> cir) {
        if (essentialPatcher$inventoryCosmeticsHidden()) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "wearablesManager", at = @At("RETURN"))
    private void essentialPatcher$refreshArmorState(CallbackInfoReturnable<WearablesManager> cir) {
        WearablesManager manager = cir.getReturnValue();
        if (manager == null) return;

        CosmeticsState state = manager.getState();
        ArmorSlots armor = essentialPatcher$currentArmor();
        if (armor.equals(state.getArmor())) return;

        manager.updateState(new CosmeticsState(
                state.getSkinType(),
                state.getCosmetics(),
                state.getBedrockModels(),
                armor
        ));
    }

    @Inject(method = "blockedArmorSlots", at = @At("HEAD"), cancellable = true)
    private void essentialPatcher$enforceArmorSetting(CallbackInfoReturnable<Set<Integer>> cir) {
        if (!essentialPatcher$cosmeticsRenderingAllowed()) {
            cir.setReturnValue(Collections.emptySet());
            return;
        }

        EssentialConfig.CosmeticOrArmor setting = essentialPatcher$armorSetting();
        if (setting != EssentialConfig.CosmeticOrArmor.ONLY_COSMETICS) {
            cir.setReturnValue(Collections.emptySet());
            return;
        }

        CosmeticsState state = ((AbstractClientPlayerExt) player).getCosmeticsState();
        Set<Integer> blockedSlots = new HashSet<>(state.getPartsEquipped());
        for (EquippedCosmetic equipped : state.getCosmetics().values()) {
            for (CosmeticProperty property : equipped.getCosmetic().getProperties()) {
                if (property instanceof CosmeticProperty.ArmorHandlingV2 armorHandling) {
                    for (List<Integer> conflicts : armorHandling.getData().getConflicts().values()) {
                        blockedSlots.addAll(conflicts);
                    }
                }
            }
        }
        cir.setReturnValue(blockedSlots);
    }

    @Unique
    private boolean essentialPatcher$inventoryCosmeticsHidden() {
        if (!GuiInventoryExt.isInventoryEntityRendering.getUntracked()) return false;
        if (EssentialConfig.INSTANCE.getDisableCosmeticsInInventory()) return true;
        return PatcherConfig.get().hideCosmeticsInInventoryWithShaders && ShaderCompat.isShaderPackActive();
    }

    @Unique
    private boolean essentialPatcher$cosmeticsRenderingAllowed() {
        return !EssentialConfig.INSTANCE.getDisableCosmetics()
                && !essentialPatcher$inventoryCosmeticsHidden()
                && !player.isSpectator()
                && !player.isInvisible();
    }

    @Unique
    private ArmorSlots essentialPatcher$currentArmor() {
        EssentialConfig.CosmeticOrArmor setting = essentialPatcher$armorSetting();
        if (setting != EssentialConfig.CosmeticOrArmor.ONLY_ARMOR) {
            return new ArmorSlots((byte) 0);
        }

        return new ArmorSlots(
                essentialPatcher$isArmorVisible(EquipmentSlot.FEET),
                essentialPatcher$isArmorVisible(EquipmentSlot.LEGS),
                essentialPatcher$isArmorVisible(EquipmentSlot.CHEST),
                essentialPatcher$isArmorVisible(EquipmentSlot.HEAD),
                essentialPatcher$isWearingAetherGloves()
        );
    }

    @Unique
    private EssentialConfig.CosmeticOrArmor essentialPatcher$armorSetting() {
        return player instanceof LocalPlayer
                ? EssentialConfig.INSTANCE.getCosmeticArmorSettingSelf()
                : EssentialConfig.INSTANCE.getCosmeticArmorSettingOther();
    }

    @Unique
    private boolean essentialPatcher$isWearingAetherGloves() {
        try {
            return essentialPatcher$aetherGlovesMethod != null
                    && (boolean) essentialPatcher$aetherGlovesMethod.invoke(null, player);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    @Unique
    private static Method essentialPatcher$findAetherGlovesMethod() {
        try {
            for (Method method : Class.forName("gg.essential.compat.aether.AetherGlovesCompat").getDeclaredMethods()) {
                if (method.getName().equals("isWearingGloves") && method.getParameterCount() == 1) {
                    method.setAccessible(true);
                    return method;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    @Unique
    private boolean essentialPatcher$isArmorVisible(EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty() || stack.getItem() instanceof RenderCosmetic || stack.is(Items.ELYTRA)) {
            return false;
        }
        return true;
    }
}
