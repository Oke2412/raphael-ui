/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Mth
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$ConfigValue
 *  net.neoforged.neoforge.common.ModConfigSpec$DoubleValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.GreatSageConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;

final class SettingsView {
    private static final float SCALE = 0.75f;
    private static final float ROW = 17.0f;
    private final List<Entry> all = new ArrayList<Entry>();
    private float panY;
    private float contentHeight;
    private float viewHeight;

    SettingsView() {
    }

    void rebuild() {
        this.all.clear();
        this.flag("enableVoice", GreatSageConfig.ENABLE_VOICE);
        this.flag("enableOverlay", GreatSageConfig.ENABLE_OVERLAY);
        this.flag("enableSfx", GreatSageConfig.ENABLE_SFX);
        this.flag("compactSkillAnnounce", GreatSageConfig.COMPACT_SKILLS);
        this.flag("showWithHiddenHud", GreatSageConfig.SHOW_WITH_HIDDEN_HUD);
        this.flag("voiceWithoutNameClip", GreatSageConfig.VOICE_WITHOUT_NAME_CLIP);
        this.number("volume", GreatSageConfig.VOLUME, 0.1);
        this.number("darkenStrength", GreatSageConfig.DARKEN_STRENGTH, 0.05);
        this.number("assessWarnRatio", GreatSageConfig.ASSESS_WARN_RATIO, 1.0);
        this.choice("voiceInput", GreatSageConfig.VOICE_INPUT, List.of("off", "wake", "pushToTalk"));
        this.flag("voiceFeedback", GreatSageConfig.VOICE_FEEDBACK);
        this.flag("voiceAutoDownload", GreatSageConfig.VOICE_AUTO_DOWNLOAD);
        this.number("voiceConfidence", GreatSageConfig.VOICE_CONFIDENCE, 0.05);
        this.integer("listenWindowTicks", GreatSageConfig.LISTEN_WINDOW_TICKS, 20.0);
        this.integer("batchWindowTicks", GreatSageConfig.BATCH_WINDOW_TICKS, 1.0);
        this.integer("maxQueued", GreatSageConfig.MAX_QUEUED, 1.0);
        this.integer("ceremonyHoldTicks", GreatSageConfig.CEREMONY_HOLD_TICKS, 10.0);
        this.list("battleSkills", GreatSageConfig.BATTLE_SKILLS);
        this.list("announcedTiers", GreatSageConfig.ANNOUNCED_TIERS);
        this.list("skillBlacklist", GreatSageConfig.SKILL_BLACKLIST);
        this.panY = 0.0f;
    }

    private void flag(String key, ModConfigSpec.BooleanValue value) {
        this.all.add(new Entry(key, Kind.FLAG, value, null, 0.0));
    }

    private void number(String key, ModConfigSpec.DoubleValue value, double step) {
        this.all.add(new Entry(key, Kind.NUMBER, value, null, step));
    }

    private void integer(String key, ModConfigSpec.IntValue value, double step) {
        this.all.add(new Entry(key, Kind.NUMBER, value, null, step));
    }

    private void choice(String key, ModConfigSpec.ConfigValue<String> value, List<String> options) {
        this.all.add(new Entry(key, Kind.CHOICE, value, options, 0.0));
    }

    private void list(String key, ModConfigSpec.ConfigValue<List<? extends String>> value) {
        this.all.add(new Entry(key, Kind.LIST, value, null, 0.0));
    }

    boolean isEmpty() {
        return this.all.isEmpty();
    }

    void pan(double dy) {
        this.panY = Mth.clamp((float)((float)((double)this.panY + dy)), (float)(-this.maxPan()), (float)0.0f);
    }

    private float maxPan() {
        return Math.max(0.0f, this.contentHeight - this.viewHeight);
    }

    boolean scrollable() {
        return this.maxPan() > 1.0f;
    }

    float scrollFraction() {
        return this.maxPan() <= 0.0f ? 0.0f : -this.panY / this.maxPan();
    }

    private static String shown(Entry entry) {
        Object raw = ((ModConfigSpec.ConfigValue)entry.value()).get();
        if (raw instanceof Boolean) {
            Boolean flag = (Boolean)raw;
            return Component.translatable((String)(flag != false ? "greatsage.settings.on" : "greatsage.settings.off")).getString();
        }
        if (raw instanceof Double) {
            Double number = (Double)raw;
            return String.format(Locale.ROOT, "%.2f", number);
        }
        if (raw instanceof List) {
            List values = (List)raw;
            return values.isEmpty() ? Component.translatable((String)"greatsage.settings.empty").getString() : values.size() + " " + Component.translatable((String)"greatsage.settings.entries").getString();
        }
        return String.valueOf(raw);
    }

    void render(GuiGraphics graphics, Font font, float left, float top, float width, float height, float alpha, double mouseX, double mouseY) {
        this.viewHeight = height;
        this.contentHeight = (float)this.all.size() * 17.0f;
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        for (int i = 0; i < this.all.size(); ++i) {
            Entry entry = this.all.get(i);
            float y = top + this.panY + (float)i * 17.0f;
            if (y < top - 17.0f || y > top + height) continue;
            boolean hover = mouseX >= (double)left && mouseX <= (double)(left + width) && mouseY >= (double)y && mouseY <= (double)(y + 17.0f);
            this.row(graphics, font, entry, left, y, width, a, hover);
        }
    }

    private void row(GuiGraphics graphics, Font font, Entry entry, float left, float y, float width, int a, boolean hover) {
        boolean editable;
        boolean bl = editable = entry.kind() != Kind.LIST;
        if (hover && editable) {
            graphics.fill(Math.round(left), Math.round(y), Math.round(left + width), Math.round(y + 17.0f - 2.0f), Math.round((float)a * 0.22f) << 24 | 0x1A3A2A);
        }
        PoseStack pose = graphics.pose();
        String label = Component.translatable((String)("greatsage.settings." + entry.key())).getString();
        pose.pushPose();
        pose.translate(left + 6.0f, y + 2.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, label, 0, 0, a << 24 | (editable ? 14679276 : 8365459), true);
        pose.popPose();
        String value = SettingsView.shown(entry);
        pose.pushPose();
        pose.translate(left + width - 6.0f, y + 2.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, value, -font.width(value), 0, a << 24 | (editable ? 0xFFFFFF : 7310462), true);
        pose.popPose();
        if (hover && editable) {
            String hint = Component.translatable((String)("greatsage.settings.hint." + entry.kind().name().toLowerCase(Locale.ROOT))).getString();
            pose.pushPose();
            pose.translate(left + width - 6.0f, y + 9.5f, 0.0f);
            pose.scale(0.6f, 0.6f, 1.0f);
            graphics.drawString(font, hint, -font.width(hint), 0, Math.round((float)a * 0.6f) << 24 | 0x7FA593, false);
            pose.popPose();
        }
    }

    boolean click(double mouseX, double mouseY, float left, float top, float width, float height, boolean secondary) {
        if (mouseX < (double)left || mouseX > (double)(left + width) || mouseY < (double)top || mouseY > (double)(top + height)) {
            return false;
        }
        int index = (int)((mouseY - (double)top - (double)this.panY) / 17.0);
        if (index < 0 || index >= this.all.size()) {
            return false;
        }
        Entry entry = this.all.get(index);
        switch (entry.kind().ordinal()) {
            case 0: {
                ModConfigSpec.BooleanValue value = (ModConfigSpec.BooleanValue)entry.value();
                value.set((Object)((Boolean)value.get() == false ? 1 : 0));
                break;
            }
            case 1: {
                double delta;
                Object raw = ((ModConfigSpec.ConfigValue)entry.value()).get();
                double d = delta = secondary ? -entry.step() : entry.step();
                if (raw instanceof Integer) {
                    Integer current = (Integer)raw;
                    ModConfigSpec.IntValue value = (ModConfigSpec.IntValue)entry.value();
                    value.set((Object)SettingsView.clamp(current + (int)delta, value));
                    break;
                }
                if (!(raw instanceof Double)) break;
                Double current = (Double)raw;
                ModConfigSpec.DoubleValue value = (ModConfigSpec.DoubleValue)entry.value();
                value.set((Object)SettingsView.clamp(current + delta, value));
                break;
            }
            case 2: {
                ModConfigSpec.ConfigValue value = (ModConfigSpec.ConfigValue)entry.value();
                List<String> options = entry.options();
                if (options == null || options.isEmpty()) break;
                int at = options.indexOf(value.get());
                int next = secondary ? (at <= 0 ? options.size() - 1 : at - 1) : (at + 1) % options.size();
                value.set((Object)options.get(next));
                break;
            }
            case 3: {
                return false;
            }
        }
        GreatSageConfig.SPEC.save();
        return true;
    }

    private static int clamp(int value, ModConfigSpec.IntValue spec) {
        return spec.getSpec().test((Object)value) ? value : (Integer)spec.get();
    }

    private static double clamp(double value, ModConfigSpec.DoubleValue spec) {
        double rounded = (double)Math.round(value * 100.0) / 100.0;
        return spec.getSpec().test((Object)rounded) ? rounded : (Double)spec.get();
    }

    private record Entry(String key, Kind kind, Object value, @Nullable List<String> options, double step) {
    }

    private static enum Kind {
        FLAG,
        NUMBER,
        CHOICE,
        LIST;

    }
}

