package com.leclowndu93150.essentialpatcher.mixin.gui;

import com.leclowndu93150.essentialpatcher.CompatibilityChecker;
import com.leclowndu93150.essentialpatcher.CompatibilityTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Unique
    private static boolean essentialPatcher$dismissed = false;

    @Unique
    private static int essentialPatcher$bannerX, essentialPatcher$bannerY;
    @Unique
    private static int essentialPatcher$bannerW, essentialPatcher$bannerH;

    @Inject(method = "render", at = @At("TAIL"))
    private void essentialPatcher$renderWarning(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (essentialPatcher$dismissed) return;
        CompatibilityChecker.runChecks();
        if (!CompatibilityTracker.needsWarning()) return;

        TitleScreen screen = (TitleScreen) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        String version = CompatibilityTracker.getDetectedVersion();
        boolean unsupported = !CompatibilityTracker.isSupported();
        List<String> failures = CompatibilityTracker.getFailedInjections();

        List<String> lines = new ArrayList<>();
        if (version != null && unsupported) {
            lines.add("§eEssential " + version + " unsupported");
        } else if (version == null) {
            lines.add("§eEssential version unknown");
        }

        if (!failures.isEmpty()) {
            lines.add("§c" + failures.size() + " patch(es) failed");
        }

        lines.add("§7Report: github.com/Leclowndu93150/Essential-Patcher");
        lines.add("§8Click to dismiss");

        int lineHeight = 10;
        int padding = 4;
        int maxTextWidth = 0;
        for (String line : lines) {
            int w = mc.font.width(line);
            if (w > maxTextWidth) maxTextWidth = w;
        }

        int bannerWidth = maxTextWidth + padding * 2;
        int bannerHeight = padding * 2 + lines.size() * lineHeight;
        int x = screen.width - bannerWidth - 4;
        int y = 4;

        essentialPatcher$bannerX = x;
        essentialPatcher$bannerY = y;
        essentialPatcher$bannerW = bannerWidth;
        essentialPatcher$bannerH = bannerHeight;

        graphics.fill(x, y, x + bannerWidth, y + bannerHeight, 0xAA000000);
        graphics.fill(x, y, x + bannerWidth, y + 1, 0xAAFF5555);

        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(mc.font,
                    Component.literal(lines.get(i)),
                    x + padding,
                    y + padding + i * lineHeight,
                    0xFFFFFF,
                    false
            );
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void essentialPatcher$onClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (essentialPatcher$dismissed) return;
        if (!CompatibilityTracker.needsWarning()) return;
        if (event.button() != 0) return;

        int mx = (int) event.x();
        int my = (int) event.y();
        if (mx >= essentialPatcher$bannerX && mx <= essentialPatcher$bannerX + essentialPatcher$bannerW
                && my >= essentialPatcher$bannerY && my <= essentialPatcher$bannerY + essentialPatcher$bannerH) {
            essentialPatcher$dismissed = true;
            cir.setReturnValue(true);
        }
    }
}
