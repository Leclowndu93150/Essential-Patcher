package com.leclowndu93150.essentialpatcher.mixin.cosmetics;

import com.leclowndu93150.essentialpatcher.httpsync.CosmeticHttpSync;
import gg.essential.cosmetics.EquippedCosmeticId;
import gg.essential.cosmetics.IngameEquippedOutfitsManager;
import gg.essential.cosmetics.OutfitUpdatesPayload;
import gg.essential.mod.Model;
import gg.essential.mod.Skin;
import gg.essential.mod.cosmetics.CosmeticSlot;
import gg.essential.mod.cosmetics.settings.CosmeticSetting;
import io.netty.buffer.ByteBuf;
import kotlinx.serialization.json.Json;
import kotlin.Pair;
import kotlin.TuplesKt;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mixin(value = OutfitUpdatesPayload.Companion.class, remap = false)
public class OutfitUpdatesPayloadMixin {

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void essentialPatcher$fixDecode(ByteBuf byteBuf, CallbackInfoReturnable<OutfitUpdatesPayload> cir) {
        try {
            cir.setReturnValue(essentialPatcher$decodeSafe(byteBuf));
        } catch (Exception e) {
            System.err.println("[EssentialPatcher] OutfitUpdatesPayload decode failed completely: " + e.getMessage());
            byteBuf.readerIndex(byteBuf.writerIndex());
            cir.setReturnValue(new OutfitUpdatesPayload(Collections.emptyList()));
        }
    }

    private static OutfitUpdatesPayload essentialPatcher$decodeSafe(ByteBuf byteBuf) {
        if (byteBuf.readByte() != 0) {
            return new OutfitUpdatesPayload(Collections.emptyList());
        }

        List<Pair<UUID, List<IngameEquippedOutfitsManager.Update>>> result = new ArrayList<>();
        List<UUID> failedUuids = new ArrayList<>();
        FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
        Model[] models = Model.values();

        while (buf.isReadable()) {
            int checkpoint = buf.readerIndex();
            UUID uuid = null;
            try {
                uuid = buf.readUUID();
                List<IngameEquippedOutfitsManager.Update> updates = new ArrayList<>();

                while (true) {
                    int type = buf.readByte() & 0xFF;

                    if (type == 0) {
                        result.add(TuplesKt.to(uuid, updates));
                        break;
                    }

                    switch (type) {
                        case 1 -> updates.add(IngameEquippedOutfitsManager.Update.Remove.INSTANCE);
                        case 2 -> {
                            CosmeticSlot slot = CosmeticSlot.Companion.of(buf.readUtf(256));
                            String id = buf.readUtf(256);
                            EquippedCosmeticId equipped = null;
                            if (!id.isEmpty()) {
                                List<CosmeticSetting> settings = new ArrayList<>();
                                while (true) {
                                    String json = buf.readUtf(32767);
                                    if (json.isEmpty()) break;
                                    Json serializer = CosmeticSetting.Companion.getJson();
                                    settings.add((CosmeticSetting) serializer.decodeFromString(
                                            CosmeticSetting.Companion.serializer(), json));
                                }
                                equipped = new EquippedCosmeticId(id, settings);
                            }
                            updates.add(new IngameEquippedOutfitsManager.Update.Cosmetic(slot, equipped));
                        }
                        case 3 -> {
                            int modelIdx = buf.readByte() & 0xFF;
                            Skin skin = null;
                            if (modelIdx != 0xFF && modelIdx < models.length) {
                                String hash = buf.readUtf(64);
                                skin = new Skin(hash, models[modelIdx]);
                            } else if (modelIdx != 0xFF) {
                                buf.readUtf(64);
                            }
                            updates.add(new IngameEquippedOutfitsManager.Update.Skin(skin));
                        }
                        case 4 -> {
                            CosmeticSlot slot = CosmeticSlot.Companion.of(buf.readUtf(256));
                            String event = buf.readUtf(256);
                            updates.add(new IngameEquippedOutfitsManager.Update.AnimationEvent(slot, event));
                        }
                        default -> throw new IllegalStateException("Unknown update type: " + type);
                    }
                }
            } catch (Exception e) {
                if (uuid != null) {
                    failedUuids.add(uuid);
                }
                buf.readerIndex(byteBuf.writerIndex());
                break;
            }
        }

        if (!failedUuids.isEmpty()) {
            CosmeticHttpSync sync = CosmeticHttpSync.get();
            if (sync.isEnabled()) {
                for (UUID failed : failedUuids) {
                    System.err.println("[EssentialPatcher] Essential payload corrupt for " + failed + ", fetching via HTTP sync");
                    sync.fetchPeerAsync(failed);
                }
            }
        }

        return new OutfitUpdatesPayload(result);
    }
}
