package com.leclowndu93150.essentialpatcher.config;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PatcherConfigScreen extends Screen {

    private final Screen parent;

    private PatcherConfigScreen(Screen parent) {
        super(Component.literal("Essential Patcher"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new PatcherConfigScreen(parent);
    }

    @Override
    protected void init() {
        PatcherConfig.get().save();
        Util.getPlatform().openFile(PatcherConfig.getConfigFile());
        Minecraft.getInstance().setScreen(parent);
    }
}
