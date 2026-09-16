/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.gui.GuiGraphics
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.dedek.greatsage.mixin;

import dev.dedek.greatsage.client.AnnouncementOverlay;
import dev.dedek.greatsage.client.AssessOverlay;
import dev.dedek.greatsage.client.SageTips;
import dev.dedek.greatsage.client.VoiceHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Gui.class})
public class GuiMixin {
    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void greatsage$renderWithHiddenHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        AssessOverlay.render(graphics);
        SageTips.render(graphics);
        VoiceHud.render(graphics);
        AnnouncementOverlay.renderWithHiddenHud(graphics, deltaTracker);
    }
}

