/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  javax.annotation.Nullable
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.client.AnnouncementOverlay;
import dev.dedek.greatsage.client.BattleSet;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

final class BattleView {
    private static final float SCALE = 0.75f;
    private static final float ROW = 14.0f;
    private final List<Entry> all = new ArrayList<Entry>();
    private String query = "";
    private float panY;
    private float contentHeight;
    private float viewHeight;

    BattleView() {
    }

    void rebuild(LocalPlayer player) {
        this.all.clear();
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom((LivingEntity)player).getLearnedSkills()) {
            String tier;
            ResourceLocation id;
            Skill skill;
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof Skill) || (skill = (Skill)manasSkill).getType() == null || (id = instance.getSkillId()) == null || "resistance".equals(tier = skill.getType().getSerializedName().toLowerCase(Locale.ROOT))) continue;
            Hold hold = instance.canBeToggled((LivingEntity)player) ? Hold.TOGGLE : Hold.RECAST;
            this.all.add(new Entry(id, (Component)skill.getName(), tier, hold));
        }
        this.all.sort(Comparator.comparingInt(e -> BattleSet.contains(e.id()) ? 0 : 1).thenComparing(e -> e.name().getString()));
        this.panY = 0.0f;
    }

    String query() {
        return this.query;
    }

    void setQuery(String value) {
        this.query = value;
        this.panY = 0.0f;
    }

    boolean isEmpty() {
        return this.all.isEmpty();
    }

    int linked() {
        return BattleSet.size();
    }

    private List<Entry> visible() {
        String needle = this.query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return this.all;
        }
        ArrayList<Entry> out = new ArrayList<Entry>();
        for (Entry entry : this.all) {
            if (!entry.name().getString().toLowerCase(Locale.ROOT).contains(needle)) continue;
            out.add(entry);
        }
        return out;
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

    void render(GuiGraphics graphics, Font font, float left, float top, float width, float height, float alpha, double mouseX, double mouseY) {
        this.viewHeight = height;
        List<Entry> rows = this.visible();
        this.contentHeight = (float)rows.size() * 14.0f;
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        int first = Math.max(0, (int)(-this.panY / 14.0f) - 1);
        int last = Math.min(rows.size(), first + (int)(height / 14.0f) + 3);
        for (int i = first; i < last; ++i) {
            Entry entry = rows.get(i);
            float y = top + this.panY + (float)i * 14.0f;
            if (y < top - 14.0f || y > top + height) continue;
            boolean hover = mouseX >= (double)left && mouseX <= (double)(left + width) && mouseY >= (double)y && mouseY <= (double)(y + 14.0f);
            this.row(graphics, font, entry, left, y, width, a, hover);
        }
    }

    private void row(GuiGraphics graphics, Font font, Entry entry, float left, float y, float width, int a, boolean hover) {
        boolean linked = BattleSet.contains(entry.id());
        boolean usable = entry.hold() != Hold.NONE;
        int tint = AnnouncementOverlay.tierColor(entry.tier());
        if (hover && usable) {
            graphics.fill(Math.round(left), Math.round(y), Math.round(left + width), Math.round(y + 14.0f - 1.0f), Math.round((float)a * 0.22f) << 24 | 0x1A3A2A);
        }
        int mark = Math.round(left + 2.0f);
        if (linked) {
            graphics.fill(mark, Math.round(y + 3.0f), mark + 3, Math.round(y + 14.0f - 3.0f), a << 24 | 0xFFFFFF);
        } else {
            graphics.fill(mark + 1, Math.round(y + 7.0f - 1.0f), mark + 2, Math.round(y + 7.0f + 1.0f), Math.round((float)a * 0.5f) << 24 | tint);
        }
        int text = usable ? (linked ? 0xFFFFFF : 14679276) : 7310462;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left + 12.0f, y + 3.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, entry.name(), 0, 0, Math.round((float)a * (usable ? 1.0f : 0.55f)) << 24 | text, true);
        pose.popPose();
        String how = Component.translatable((String)("greatsage.battle.hold." + entry.hold().name().toLowerCase(Locale.ROOT))).getString();
        pose.pushPose();
        pose.translate(left + width - 6.0f, y + 3.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, how, -font.width(how), 0, Math.round((float)a * (linked ? 0.9f : 0.5f)) << 24 | tint, true);
        pose.popPose();
    }

    @Nullable
    Boolean click(double mouseX, double mouseY, float left, float top, float width, float height) {
        if (mouseX < (double)left || mouseX > (double)(left + width) || mouseY < (double)top || mouseY > (double)(top + height)) {
            return null;
        }
        List<Entry> rows = this.visible();
        int index = (int)((mouseY - (double)top - (double)this.panY) / 14.0);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        Entry entry = rows.get(index);
        if (entry.hold() == Hold.NONE) {
            return null;
        }
        BattleSet.toggle(entry.id());
        return BattleSet.contains(entry.id());
    }

    @Nullable
    ResourceLocation at(double mouseX, double mouseY, float left, float top, float width, float height) {
        if (mouseX < (double)left || mouseX > (double)(left + width) || mouseY < (double)top || mouseY > (double)(top + height)) {
            return null;
        }
        List<Entry> rows = this.visible();
        int index = (int)((mouseY - (double)top - (double)this.panY) / 14.0);
        return index >= 0 && index < rows.size() ? rows.get(index).id() : null;
    }

    private static enum Hold {
        TOGGLE,
        RECAST,
        NONE;

    }

    private record Entry(ResourceLocation id, Component name, String tier, Hold hold) {
    }
}

