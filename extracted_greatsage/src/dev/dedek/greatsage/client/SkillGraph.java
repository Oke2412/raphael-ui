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
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.client.AnnouncementOverlay;
import dev.dedek.greatsage.client.LightFx;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

final class SkillGraph {
    private static final float SKILL_SCALE = 0.75f;
    private static final float GAP = 20.0f;
    private static final float DRIFT = 1.8f;
    private final Map<String, List<Node>> byTier = new LinkedHashMap<String, List<Node>>();
    private final List<String> tiers = new ArrayList<String>();
    @Nullable
    private String expanded;
    @Nullable
    private Object hovered;
    private String query = "";
    private float panY;
    private float contentHeight;
    private float viewHeight;
    private final List<float[]> hitBoxes = new ArrayList<float[]>();
    private final List<Object> hitTargets = new ArrayList<Object>();

    SkillGraph() {
    }

    void rebuild(LocalPlayer player) {
        this.byTier.clear();
        this.tiers.clear();
        LinkedHashMap<String, List> gathered = new LinkedHashMap<String, List>();
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom((LivingEntity)player).getLearnedSkills()) {
            ResourceLocation id;
            Skill skill;
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof Skill) || (skill = (Skill)manasSkill).getType() == null || (id = skill.getRegistryName()) == null) continue;
            String tier2 = skill.getType().getSerializedName().toLowerCase(Locale.ROOT);
            gathered.computeIfAbsent(tier2, key -> new ArrayList()).add(new Node(id, (Component)skill.getName(), tier2, instance.isMastered((LivingEntity)player)));
        }
        for (String raw : (List)GreatSageConfig.TIER_ORDER.get()) {
            String tier3 = raw.trim().toLowerCase(Locale.ROOT);
            List group2 = (List)gathered.remove(tier3);
            if (group2 == null || group2.isEmpty()) continue;
            this.byTier.put(tier3, group2);
            this.tiers.add(tier3);
        }
        gathered.forEach((tier, group) -> {
            if (!group.isEmpty()) {
                this.byTier.put((String)tier, (List<Node>)group);
                this.tiers.add((String)tier);
            }
        });
        this.expanded = null;
        this.panY = 0.0f;
    }

    boolean isEmpty() {
        return this.tiers.isEmpty();
    }

    boolean hasAnalysable() {
        for (String tier : this.tiers) {
            if (!this.hasAnalysable(tier)) continue;
            return true;
        }
        return false;
    }

    boolean hasAnalysable(String tier) {
        for (Node node : this.byTier.getOrDefault(tier, List.of())) {
            if (!node.analysable()) continue;
            return true;
        }
        return false;
    }

    @Nullable
    String expanded() {
        return this.expanded;
    }

    void collapse() {
        this.expanded = null;
        this.panY = 0.0f;
    }

    void expand(String tier) {
        this.expanded = tier.equals(this.expanded) ? null : tier;
        this.panY = 0.0f;
    }

    String query() {
        return this.query;
    }

    void setQuery(String value) {
        this.query = value;
        this.panY = 0.0f;
    }

    private List<Node> matches() {
        String needle = this.query.trim().toLowerCase(Locale.ROOT);
        ArrayList<Node> out = new ArrayList<Node>();
        for (String tier : this.tiers) {
            for (Node node : this.byTier.get(tier)) {
                if (!node.name().getString().toLowerCase(Locale.ROOT).contains(needle)) continue;
                out.add(node);
            }
        }
        return out;
    }

    boolean searching() {
        return !this.query.trim().isEmpty();
    }

    void pan(double dy) {
        this.panY = Mth.clamp((float)((float)((double)this.panY + dy)), (float)(-this.maxPan()), (float)this.maxPan());
    }

    void resetPan() {
        this.panY = 0.0f;
    }

    private float maxPan() {
        return Math.max(0.0f, (this.contentHeight - this.viewHeight) / 2.0f + 24.0f);
    }

    boolean scrollable() {
        return this.maxPan() > 1.0f;
    }

    float scrollFraction() {
        float span = this.maxPan() * 2.0f;
        return span <= 0.0f ? 0.0f : (this.panY + this.maxPan()) / span;
    }

    void render(GuiGraphics graphics, Font font, float cx, float cy, float areaWidth, float areaHeight, float time, float alpha, @Nullable ResourceLocation analysing, double mouseX, double mouseY) {
        this.hovered = this.pick(mouseX, mouseY);
        this.hitBoxes.clear();
        this.hitTargets.clear();
        this.viewHeight = areaHeight;
        if (this.searching()) {
            this.grid(graphics, font, this.matches(), cx, cy, areaWidth, areaHeight, time, alpha, analysing, null);
        } else if (this.expanded == null) {
            this.contentHeight = 0.0f;
            this.renderTiers(graphics, font, cx, cy, Math.min(areaWidth, areaHeight) * 0.4f, time, alpha);
        } else {
            this.grid(graphics, font, this.byTier.getOrDefault(this.expanded, List.of()), cx, cy, areaWidth, areaHeight, time, alpha, analysing, this.expanded);
        }
    }

    private void renderTiers(GuiGraphics graphics, Font font, float cx, float cy, float radius, float time, float alpha) {
        int i;
        LightFx.beginAdditive();
        LightFx.glow(graphics, cx, cy, 24.0f, 14679276, alpha * 0.35f);
        LightFx.glow(graphics, cx, cy, 8.0f, 0xFFFFFF, alpha * 0.8f);
        for (i = 0; i < this.tiers.size(); ++i) {
            float[] p = this.tierPos(i, cx, cy, radius, time);
            this.thread(graphics, cx, cy, p[0], p[1], time, i, alpha * 0.5f);
        }
        LightFx.endAdditive(graphics);
        for (i = 0; i < this.tiers.size(); ++i) {
            String tier = this.tiers.get(i);
            float[] p = this.tierPos(i, cx, cy, radius, time);
            int left = this.byTier.get(tier).size() - this.analysedCount(tier);
            String text = Component.translatable((String)("greatsage.tier." + tier)).getString().toUpperCase(Locale.ROOT) + "  " + left;
            int color = AnnouncementOverlay.tierColor(tier);
            float dim = this.hasAnalysable(tier) ? 1.0f : 0.45f;
            this.node(graphics, font, p[0], p[1], text, color, alpha * dim, false, 0.9f, tier.equals(this.hovered));
            float f = p[0];
            float f2 = p[1];
            float f3 = (float)font.width(text) * 0.9f;
            Objects.requireNonNull(font);
            this.record(f, f2, f3, 9.0f * 0.9f, tier);
        }
    }

    private int analysedCount(String tier) {
        int done = 0;
        for (Node node : this.byTier.getOrDefault(tier, List.of())) {
            if (!node.mastered()) continue;
            ++done;
        }
        return done;
    }

    private void grid(GuiGraphics graphics, Font font, List<Node> nodes, float cx, float cy, float areaWidth, float areaHeight, float time, float alpha, @Nullable ResourceLocation analysing, @Nullable String tier) {
        Objects.requireNonNull(font);
        float rowHeight = 9.0f * 0.75f + 15.0f;
        ArrayList rows = new ArrayList();
        ArrayList<Float> widths = new ArrayList<Float>();
        ArrayList<Node> row = new ArrayList<Node>();
        float used = 0.0f;
        for (Node node : nodes) {
            float w = (float)font.width(node.name().getString()) * 0.75f + 20.0f;
            if (!row.isEmpty() && used + w > areaWidth) {
                rows.add(row);
                widths.add(Float.valueOf(used));
                row = new ArrayList();
                used = 0.0f;
            }
            row.add(node);
            used += w;
        }
        if (!row.isEmpty()) {
            rows.add(row);
            widths.add(Float.valueOf(used));
        }
        this.contentHeight = (float)rows.size() * rowHeight;
        float top = cy - this.contentHeight / 2.0f + rowHeight / 2.0f + this.panY;
        ArrayList<float[]> spots = new ArrayList<float[]>();
        ArrayList<Node> flat = new ArrayList<Node>();
        for (int r = 0; r < rows.size(); ++r) {
            List current = (List)rows.get(r);
            float rowWidth = ((Float)widths.get(r)).floatValue();
            float cursor = cx - rowWidth / 2.0f;
            ArrayList ordered = new ArrayList(current);
            if (r % 2 == 1) {
                Collections.reverse(ordered);
            }
            for (Node node : ordered) {
                float w = (float)font.width(node.name().getString()) * 0.75f + 20.0f;
                spots.add(this.drift(cursor + w / 2.0f, top + (float)r * rowHeight, time, flat.size()));
                flat.add(node);
                cursor += w;
            }
        }
        float headY = cy - this.contentHeight / 2.0f - 26.0f + this.panY;
        LightFx.beginAdditive();
        if (!spots.isEmpty()) {
            this.thread(graphics, cx, headY, ((float[])spots.get(0))[0], ((float[])spots.get(0))[1], time, 0, alpha * 0.4f);
            for (int i = 1; i < spots.size(); ++i) {
                this.thread(graphics, ((float[])spots.get(i - 1))[0], ((float[])spots.get(i - 1))[1], ((float[])spots.get(i))[0], ((float[])spots.get(i))[1], time, i, alpha * 0.26f);
            }
        }
        LightFx.endAdditive(graphics);
        String head = tier != null ? Component.translatable((String)("greatsage.tier." + tier)).getString().toUpperCase(Locale.ROOT) : Component.translatable((String)"greatsage.menu.search.results", (Object[])new Object[]{nodes.size()}).getString();
        int headColor = tier != null ? AnnouncementOverlay.tierColor(tier) : 14679276;
        this.node(graphics, font, cx, headY, head, headColor, alpha, true, 0.9f, tier != null && tier.equals(this.hovered));
        if (tier != null) {
            float f = (float)font.width(head) * 0.9f;
            Objects.requireNonNull(font);
            this.record(cx, headY, f, 9.0f * 0.9f, tier);
        }
        for (int i = 0; i < flat.size(); ++i) {
            boolean busy;
            Node skill = (Node)flat.get(i);
            float[] p = (float[])spots.get(i);
            String text = skill.name().getString();
            boolean bl = busy = analysing != null && analysing.equals((Object)skill.id());
            int tint = busy ? 0xFFFFFF : (skill.mastered() ? 7310462 : AnnouncementOverlay.tierColor(skill.tier()));
            this.node(graphics, font, p[0], p[1], text, tint, alpha * (skill.mastered() ? 0.5f : 1.0f), busy, 0.75f, skill.equals(this.hovered));
            float f = p[0];
            float f2 = p[1];
            float f3 = (float)font.width(text) * 0.75f;
            Objects.requireNonNull(font);
            this.record(f, f2, f3, 9.0f * 0.75f, skill);
        }
    }

    private void node(GuiGraphics graphics, Font font, float x, float y, String text, int color, float alpha, boolean strong, float scale, boolean hover) {
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0f);
        pose.scale(scale * (hover ? 1.1f : 1.0f), scale * (hover ? 1.1f : 1.0f), 1.0f);
        int w = font.width(text);
        int left = -w / 2;
        Objects.requireNonNull(font);
        int top = -9 / 2;
        Objects.requireNonNull(font);
        int bottom = top + 9 + 1;
        graphics.fill(left - 4, top - 2, left + w + 4, bottom, Math.round((float)a * (hover ? 0.86f : 0.66f)) << 24 | 0x4140E);
        if (hover) {
            int edge = a << 24 | color;
            graphics.fill(left - 4, top - 2, left + w + 4, top - 1, edge);
            graphics.fill(left - 4, bottom - 1, left + w + 4, bottom, edge);
            graphics.fill(left - 4, top - 2, left - 3, bottom, edge);
            graphics.fill(left + w + 3, top - 2, left + w + 4, bottom, edge);
        } else if (strong) {
            int edge = Math.round((float)a * 0.75f) << 24 | color;
            graphics.fill(left - 4, top - 2, left + w + 4, top - 1, edge);
            graphics.fill(left - 4, bottom - 1, left + w + 4, bottom, edge);
        }
        graphics.drawString(font, text, left, top, a << 24 | (hover ? 0xFFFFFF : color), true);
        pose.popPose();
    }

    private void thread(GuiGraphics graphics, float ax, float ay, float bx, float by, float time, int seed, float alpha) {
        float sag = 7.0f + 3.5f * Mth.sin((float)(time * 9.0E-4f + (float)seed * 0.7f));
        float mx = (ax + bx) * 0.5f;
        float my = (ay + by) * 0.5f + sag;
        int steps = 10;
        for (int i = 1; i < steps; ++i) {
            float t = (float)i / (float)steps;
            float inv = 1.0f - t;
            float x = inv * inv * ax + 2.0f * inv * t * mx + t * t * bx;
            float y = inv * inv * ay + 2.0f * inv * t * my + t * t * by;
            float ends = Math.min(1.0f, (float)Math.min(i, steps - i) / 3.0f);
            LightFx.glow(graphics, x, y, 2.4f, 9431488, alpha * ends);
        }
    }

    private float[] tierPos(int index, float cx, float cy, float radius, float time) {
        float angle = -90.0f + (float)index * (360.0f / (float)Math.max(1, this.tiers.size()));
        float rad = angle * ((float)Math.PI / 180);
        return this.drift(cx + Mth.cos((float)rad) * radius, cy + Mth.sin((float)rad) * radius * 0.82f, time, index);
    }

    private float[] drift(float x, float y, float time, int seed) {
        float phase = (float)seed * 0.7f;
        return new float[]{x + Mth.sin((float)(time * 4.2E-4f + phase)) * 1.8f, y + Mth.cos((float)(time * 3.6E-4f + phase * 1.3f)) * 1.8f};
    }

    private void record(float x, float y, float width, float height, Object target) {
        this.hitBoxes.add(new float[]{x - width / 2.0f - 5.0f, y - height / 2.0f - 4.0f, width + 10.0f, height + 8.0f});
        this.hitTargets.add(target);
    }

    @Nullable
    Object pick(double mouseX, double mouseY) {
        for (int i = 0; i < this.hitBoxes.size(); ++i) {
            float[] box = this.hitBoxes.get(i);
            if (!(mouseX >= (double)box[0]) || !(mouseX <= (double)(box[0] + box[2])) || !(mouseY >= (double)box[1]) || !(mouseY <= (double)(box[1] + box[3]))) continue;
            return this.hitTargets.get(i);
        }
        return null;
    }

    record Node(ResourceLocation id, Component name, String tier, boolean mastered) {
        boolean analysable() {
            return !this.mastered && !"resistance".equals(this.tier);
        }
    }
}

