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
 *  net.neoforged.fml.ModList
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;

final class SageFilters {
    private static final float SCALE = 0.7f;
    private static final float CHIP_H = 13.0f;
    private static final float GAP = 3.0f;
    private static final float TITLE_H = 11.0f;
    private static final float BLOCK_GAP = 7.0f;
    private static final float PAD = 8.0f;
    static final int NO_TINT = 0;
    private final List<Facet> facets = new ArrayList<Facet>();
    private final Runnable onChange;
    private float pan;

    SageFilters(Runnable onChange) {
        this.onChange = onChange;
    }

    Facet add(String titleKey, boolean multi, List<Option> options) {
        Facet facet = new Facet(Component.translatable((String)titleKey).getString().toUpperCase(Locale.ROOT), multi, options);
        this.facets.add(facet);
        return facet;
    }

    void clear() {
        this.facets.clear();
    }

    boolean isEmpty() {
        return this.facets.isEmpty();
    }

    boolean active() {
        for (Facet facet : this.facets) {
            if (!facet.touched()) continue;
            return true;
        }
        return false;
    }

    void reset() {
        for (Facet facet : this.facets) {
            facet.chosen.clear();
            if (facet.multi || facet.options.isEmpty()) continue;
            facet.chosen.add(facet.options.get(0).value());
        }
        this.onChange.run();
    }

    private List<Slot> layout(Font font, float left, float top, float width) {
        ArrayList<Slot> slots = new ArrayList<Slot>();
        float limit = left + width - 8.0f;
        float y = top + 8.0f + this.pan;
        for (Facet facet : this.facets) {
            slots.add(new Slot(facet, null, left + 8.0f, y, 0.0f));
            y += 11.0f;
            float x = left + 8.0f;
            for (Option option : facet.options) {
                float w = SageFilters.chipWidth(font, option);
                if (x + w > limit && x > left + 8.0f) {
                    x = left + 8.0f;
                    y += 16.0f;
                }
                slots.add(new Slot(facet, option, x, y, w));
                x += w + 3.0f;
            }
            y += 20.0f;
        }
        return slots;
    }

    private static float chipWidth(Font font, Option option) {
        return (float)font.width(option.label()) * 0.7f + 12.0f + (option.tint() == 0 ? 0.0f : 7.0f);
    }

    float contentHeight(Font font, float width) {
        float saved = this.pan;
        this.pan = 0.0f;
        float bottom = 8.0f;
        for (Slot slot : this.layout(font, 0.0f, 0.0f, width)) {
            bottom = Math.max(bottom, slot.y() + (slot.option() == null ? 11.0f : 13.0f));
        }
        this.pan = saved;
        return bottom + 8.0f;
    }

    void pan(double dy, Font font, float width, float viewHeight) {
        float max = Math.max(0.0f, this.contentHeight(font, width) - viewHeight);
        this.pan = Mth.clamp((float)((float)((double)this.pan + dy)), (float)(-max), (float)0.0f);
    }

    void render(GuiGraphics graphics, Font font, float left, float top, float width, float height, float anchorX, float alpha, double mouseX, double mouseY) {
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a < 8 || this.facets.isEmpty()) {
            return;
        }
        int l = Math.round(left);
        int t = Math.round(top);
        int w = Math.round(width);
        int h = Math.round(height);
        graphics.fill(l, t, l + w, t + h, Math.round((float)a * 0.94f) << 24 | 0x4140E);
        int edge = Math.round((float)a * 0.6f) << 24 | 0x8FE9C0;
        graphics.fill(l, t, l + w, t + 1, edge);
        graphics.fill(l, t + h - 1, l + w, t + h, edge);
        graphics.fill(l, t, l + 1, t + h, edge);
        graphics.fill(l + w - 1, t, l + w, t + h, edge);
        for (int i = 0; i < 3; ++i) {
            graphics.fill(Math.round(anchorX) - 3 + i, t - 3 + i, Math.round(anchorX) + 3 - i, t - 2 + i, edge);
        }
        PoseStack pose = graphics.pose();
        for (Slot slot : this.layout(font, left, top, width)) {
            if (slot.y() < top || slot.y() + 13.0f > top + height) continue;
            if (slot.option() == null) {
                pose.pushPose();
                pose.translate(slot.x(), slot.y() + 1.0f, 0.0f);
                pose.scale(0.65f, 0.65f, 1.0f);
                graphics.drawString(font, slot.facet().title, 0, 0, Math.round((float)a * 0.55f) << 24 | 0x7FA593, false);
                pose.popPose();
                continue;
            }
            boolean on = slot.facet().chosen.contains(slot.option().value());
            boolean hover = mouseX >= (double)slot.x() && mouseX <= (double)(slot.x() + slot.w()) && mouseY >= (double)slot.y() && mouseY <= (double)(slot.y() + 13.0f);
            this.chip(graphics, font, pose, slot, on, hover, a);
        }
        float content = this.contentHeight(font, width);
        if (content > height + 1.0f) {
            int rail = l + w - 3;
            graphics.fill(rail, t + 4, rail + 1, t + h - 4, Math.round((float)a * 0.25f) << 24 | 0xFFFFFF);
            int span = h - 8;
            int thumb = Math.max(12, Math.round((float)span * height / content));
            int y = t + 4 + Math.round((float)(span - thumb) * (-this.pan / (content - height)));
            graphics.fill(rail - 1, y, rail + 2, y + thumb, a << 24 | 0x8FE9C0);
        }
    }

    private void chip(GuiGraphics graphics, Font font, PoseStack pose, Slot slot, boolean on, boolean hover, int a) {
        Option option = slot.option();
        int tint = option.tint() == 0 ? 14679276 : option.tint();
        int x = Math.round(slot.x());
        int y = Math.round(slot.y());
        int w = Math.round(slot.w());
        int h = Math.round(13.0f);
        float fill = on ? 0.34f : (hover ? 0.28f : 0.16f);
        graphics.fill(x, y, x + w, y + h, Math.round((float)a * fill) << 24 | 0x61C13);
        if (on) {
            graphics.fill(x, y, x + w, y + h, Math.round((float)a * 0.22f) << 24 | tint);
        }
        int edge = Math.round((float)a * (on ? 0.85f : (hover ? 0.5f : 0.25f))) << 24 | tint;
        graphics.fill(x, y, x + w, y + 1, edge);
        graphics.fill(x, y + h - 1, x + w, y + h, edge);
        graphics.fill(x, y, x + 1, y + h, edge);
        graphics.fill(x + w - 1, y, x + w, y + h, edge);
        int textLeft = x + 6;
        if (option.tint() != 0) {
            graphics.fill(textLeft, y + 4, textLeft + 2, y + h - 4, Math.round((float)a * (on ? 1.0f : 0.6f)) << 24 | option.tint());
            textLeft += 7;
        }
        pose.pushPose();
        float f = textLeft;
        float f2 = y;
        Objects.requireNonNull(font);
        pose.translate(f, f2 + (13.0f - 9.0f * 0.7f) / 2.0f + 0.5f, 0.0f);
        pose.scale(0.7f, 0.7f, 1.0f);
        graphics.drawString(font, option.label(), 0, 0, Math.round((float)a * (on ? 1.0f : 0.8f)) << 24 | (on ? 0xFFFFFF : (hover ? 14679276 : 9414298)), false);
        pose.popPose();
    }

    boolean click(Font font, float left, float top, float width, float height, double mouseX, double mouseY) {
        for (Slot slot : this.layout(font, left, top, width)) {
            Option option = slot.option();
            if (option == null || slot.y() < top || slot.y() + 13.0f > top + height || !(mouseX >= (double)slot.x()) || !(mouseX <= (double)(slot.x() + slot.w())) || !(mouseY >= (double)slot.y()) || !(mouseY <= (double)(slot.y() + 13.0f))) continue;
            this.toggle(slot.facet(), option);
            return true;
        }
        return false;
    }

    private void toggle(Facet facet, Option option) {
        if (facet.multi) {
            if (!facet.chosen.remove(option.value())) {
                facet.chosen.add(option.value());
            }
        } else {
            facet.chosen.clear();
            facet.chosen.add(option.value());
        }
        this.onChange.run();
    }

    static String modName(String namespace) {
        String display = ModList.get().getModContainerById(namespace).map(container -> container.getModInfo().getDisplayName()).orElse(namespace);
        return display.length() > 22 ? display.substring(0, 21) + "\u2026" : display;
    }

    static List<Option> order(boolean descendingFirst) {
        Option asc = new Option("asc", Component.translatable((String)"greatsage.filter.order.asc").getString());
        Option desc = new Option("desc", Component.translatable((String)"greatsage.filter.order.desc").getString());
        return descendingFirst ? List.of(desc, asc) : List.of(asc, desc);
    }

    static final class Facet {
        private final String title;
        private final boolean multi;
        private final List<Option> options;
        private final Set<String> chosen = new LinkedHashSet<String>();

        private Facet(String title, boolean multi, List<Option> options) {
            this.title = title;
            this.multi = multi;
            this.options = options;
            if (!multi && !options.isEmpty()) {
                this.chosen.add(options.get(0).value());
            }
        }

        String single() {
            return this.chosen.isEmpty() ? "" : this.chosen.iterator().next();
        }

        boolean accepts(String value) {
            return this.chosen.isEmpty() || this.chosen.contains(value);
        }

        boolean acceptsAny(Collection<String> values) {
            if (this.chosen.isEmpty()) {
                return true;
            }
            for (String value : values) {
                if (!this.chosen.contains(value)) continue;
                return true;
            }
            return false;
        }

        private boolean touched() {
            return this.multi ? !this.chosen.isEmpty() : !this.options.isEmpty() && !this.chosen.contains(this.options.get(0).value());
        }
    }

    record Option(String value, String label, int tint) {
        Option(String value, String label) {
            this(value, label, 0);
        }
    }

    private record Slot(Facet facet, @Nullable Option option, float x, float y, float w) {
    }
}

