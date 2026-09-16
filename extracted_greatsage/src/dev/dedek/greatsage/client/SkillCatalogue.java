/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.manascore.skill.impl.SkillStorage
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
import dev.dedek.greatsage.client.SageFilters;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
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

final class SkillCatalogue {
    private static final float SCALE = 0.75f;
    private static final float ROW = 13.0f;
    private final List<Entry> all = new ArrayList<Entry>();
    private final List<String> tierOrder = new ArrayList<String>();
    private final SageFilters filters = new SageFilters(this::invalidate);
    @Nullable
    private SageFilters.Facet tierFacet;
    @Nullable
    private SageFilters.Facet sourceFacet;
    @Nullable
    private SageFilters.Facet stateFacet;
    @Nullable
    private SageFilters.Facet sortFacet;
    @Nullable
    private SageFilters.Facet orderFacet;
    @Nullable
    private List<Entry> shown;
    private String query = "";
    @Nullable
    private Entry selected;
    private float panY;
    private float contentHeight;
    private float viewHeight;

    SkillCatalogue() {
    }

    SageFilters filters() {
        return this.filters;
    }

    private void invalidate() {
        this.shown = null;
        this.panY = 0.0f;
    }

    void rebuild(LocalPlayer player) {
        this.all.clear();
        SkillStorage storage = SkillAPI.getSkillsFrom((LivingEntity)player);
        for (ManasSkill entry : SkillAPI.getSkillRegistry()) {
            ResourceLocation id;
            Skill skill;
            if (!(entry instanceof Skill) || (skill = (Skill)entry).getType() == null || (id = skill.getRegistryName()) == null) continue;
            ManasSkillInstance owned = storage.getSkill(id).orElse(null);
            this.all.add(new Entry(id, (Component)skill.getName(), skill.getType().getSerializedName().toLowerCase(Locale.ROOT), owned != null, owned == null ? 0.0 : owned.getMastery(), SkillCatalogue.maxMastery(skill), SkillCatalogue.cost(skill, owned)));
        }
        this.tierOrder.clear();
        for (String tier : (List)GreatSageConfig.TIER_ORDER.get()) {
            this.tierOrder.add(tier.trim().toLowerCase(Locale.ROOT));
        }
        this.all.sort(this.byTier().thenComparing(e -> e.name().getString()));
        this.buildFilters();
        this.panY = 0.0f;
        this.selected = null;
        this.shown = null;
    }

    private static double cost(Skill skill, @Nullable ManasSkillInstance owned) {
        double real;
        if (owned != null && (real = skill.getAcquiringMagiculeCost(owned)) > 0.0) {
            return real;
        }
        double cached = skill.getCachedAcquiringMpCost();
        return cached > 0.0 ? cached : skill.getDefaultAcquiringMagiculeCost();
    }

    private static int maxMastery(Skill skill) {
        int cached = skill.getCachedMaxMastery();
        return cached > 0 ? cached : skill.getMaxMastery();
    }

    private Comparator<Entry> byTier() {
        return Comparator.comparingInt(entry -> {
            int index = this.tierOrder.indexOf(entry.tier());
            return index < 0 ? this.tierOrder.size() : index;
        });
    }

    private void buildFilters() {
        this.filters.clear();
        ArrayList<String> tiers = new ArrayList<String>();
        ArrayList<String> sources = new ArrayList<String>();
        for (Entry entry : this.all) {
            String namespace;
            if (!tiers.contains(entry.tier())) {
                tiers.add(entry.tier());
            }
            if (sources.contains(namespace = entry.id().getNamespace())) continue;
            sources.add(namespace);
        }
        tiers.sort(Comparator.comparingInt(tier -> {
            int index = this.tierOrder.indexOf(tier);
            return index < 0 ? this.tierOrder.size() : index;
        }));
        sources.sort(Comparator.naturalOrder());
        ArrayList<SageFilters.Option> tierOptions = new ArrayList<SageFilters.Option>();
        for (String tier2 : tiers) {
            tierOptions.add(new SageFilters.Option(tier2, Component.translatable((String)("greatsage.filter.tier." + tier2)).getString(), AnnouncementOverlay.tierColor(tier2)));
        }
        this.tierFacet = this.filters.add("greatsage.filter.tier", true, tierOptions);
        ArrayList<SageFilters.Option> arrayList = new ArrayList<SageFilters.Option>();
        for (String namespace : sources) {
            arrayList.add(new SageFilters.Option(namespace, SageFilters.modName(namespace)));
        }
        this.sourceFacet = this.filters.add("greatsage.filter.source", true, arrayList);
        this.stateFacet = this.filters.add("greatsage.filter.state", false, List.of(SkillCatalogue.state("all"), SkillCatalogue.state("owned"), SkillCatalogue.state("missing"), SkillCatalogue.state("mastered"), SkillCatalogue.state("learning")));
        this.sortFacet = this.filters.add("greatsage.filter.sort", false, List.of(SkillCatalogue.sort("tier"), SkillCatalogue.sort("name"), SkillCatalogue.sort("mastery"), SkillCatalogue.sort("cost")));
        this.orderFacet = this.filters.add("greatsage.filter.order", false, SageFilters.order(false));
    }

    private static SageFilters.Option state(String value) {
        return new SageFilters.Option(value, Component.translatable((String)("greatsage.filter.state." + value)).getString());
    }

    private static SageFilters.Option sort(String value) {
        return new SageFilters.Option(value, Component.translatable((String)("greatsage.filter.sort." + value)).getString());
    }

    boolean isEmpty() {
        return this.all.isEmpty();
    }

    int total() {
        return this.all.size();
    }

    void setQuery(String value) {
        this.query = value;
        this.invalidate();
    }

    String query() {
        return this.query;
    }

    int shownCount() {
        return this.visible().size();
    }

    private List<Entry> visible() {
        if (this.shown != null) {
            return this.shown;
        }
        String needle = this.query.trim().toLowerCase(Locale.ROOT);
        ArrayList<Entry> out = new ArrayList<Entry>();
        for (Entry entry : this.all) {
            if (!needle.isEmpty() && !entry.name().getString().toLowerCase(Locale.ROOT).contains(needle) && !entry.id().getPath().contains(needle) || this.tierFacet != null && !this.tierFacet.accepts(entry.tier()) || this.sourceFacet != null && !this.sourceFacet.accepts(entry.id().getNamespace()) || this.stateFacet != null && !SkillCatalogue.matchesState(entry, this.stateFacet.single())) continue;
            out.add(entry);
        }
        out.sort(this.comparator());
        this.shown = out;
        return out;
    }

    private static boolean matchesState(Entry entry, String state) {
        return switch (state) {
            case "owned" -> entry.owned();
            case "missing" -> {
                if (!entry.owned()) {
                    yield true;
                }
                yield false;
            }
            case "mastered" -> {
                if (entry.owned() && entry.ratio() >= 1.0f) {
                    yield true;
                }
                yield false;
            }
            case "learning" -> {
                if (entry.owned() && entry.ratio() < 1.0f) {
                    yield true;
                }
                yield false;
            }
            default -> true;
        };
    }

    private Comparator<Entry> comparator() {
        Comparator<Entry> alpha = Comparator.comparing(entry -> entry.name().getString().toLowerCase(Locale.ROOT));
        Comparator<Entry> base = switch (this.sortFacet == null ? "tier" : this.sortFacet.single()) {
            case "name" -> alpha;
            case "mastery" -> Comparator.comparingDouble(entry -> entry.ratio()).thenComparing(alpha);
            case "cost" -> Comparator.comparingDouble(entry -> entry.cost()).thenComparing(alpha);
            default -> this.byTier().thenComparing(alpha);
        };
        return this.orderFacet != null && "desc".equals(this.orderFacet.single()) ? base.reversed() : base;
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
        this.contentHeight = (float)rows.size() * 13.0f;
        int first = Math.max(0, (int)(-this.panY / 13.0f) - 1);
        int last = Math.min(rows.size(), first + (int)(height / 13.0f) + 3);
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        for (int i = first; i < last; ++i) {
            Entry entry = rows.get(i);
            float y = top + this.panY + (float)i * 13.0f;
            if (y < top - 13.0f || y > top + height) continue;
            boolean hover = mouseX >= (double)left && mouseX <= (double)(left + width) && mouseY >= (double)y && mouseY <= (double)(y + 13.0f);
            this.row(graphics, font, entry, left, y, width, a, hover);
        }
    }

    private void row(GuiGraphics graphics, Font font, Entry entry, float left, float y, float width, int a, boolean hover) {
        String right;
        int alpha;
        int tint = AnnouncementOverlay.tierColor(entry.tier());
        int n = alpha = entry.owned() ? a : Math.round((float)a * 0.5f);
        if (hover) {
            graphics.fill(Math.round(left), Math.round(y), Math.round(left + width), Math.round(y + 13.0f - 1.0f), Math.round((float)a * 0.22f) << 24 | 0x1A3A2A);
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left + 4.0f, y + 2.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, entry.name(), 0, 0, alpha << 24 | (entry.owned() ? 14679276 : 9414298), true);
        pose.popPose();
        String string = right = entry.owned() ? Math.round(entry.ratio() * 100.0f) + "%" : SkillCatalogue.formatCost(entry.cost());
        int rightColor = entry.owned() ? (entry.ratio() >= 1.0f ? 0xFFFFFF : 9431488) : 10135748;
        pose.pushPose();
        pose.translate(left + width - 6.0f, y + 2.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, right, -font.width(right), 0, alpha << 24 | rightColor, true);
        pose.popPose();
        graphics.fill(Math.round(left - 4.0f), Math.round(y + 3.0f), Math.round(left - 2.0f), Math.round(y + 13.0f - 3.0f), alpha << 24 | tint);
        if (entry.owned() && entry.ratio() > 0.0f) {
            int barLeft = Math.round(left + 4.0f);
            int barWidth = Math.round(width * 0.42f);
            graphics.fill(barLeft, Math.round(y + 13.0f - 3.0f), barLeft + Math.round((float)barWidth * entry.ratio()), Math.round(y + 13.0f - 2.0f), alpha << 24 | tint);
        }
    }

    private static String formatCost(double cost) {
        if (cost <= 0.0) {
            return "\u2014";
        }
        return cost >= 1000.0 ? Math.round(cost / 1000.0) + "k MP" : Math.round(cost) + " MP";
    }

    void select(@Nullable Entry entry) {
        this.selected = entry != null && entry.equals(this.selected) ? null : entry;
    }

    @Nullable
    Entry selected() {
        return this.selected;
    }

    void clearSelection() {
        this.selected = null;
    }

    void renderDetail(GuiGraphics graphics, Font font, float left, float top, float width, float height, float alpha) {
        if (this.selected == null) {
            return;
        }
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        int l = Math.round(left);
        int t = Math.round(top);
        int w = Math.round(width);
        int h = Math.round(height);
        int tint = AnnouncementOverlay.tierColor(this.selected.tier());
        graphics.fill(l, t, l + w, t + h, Math.round((float)a * 0.9f) << 24 | 0x4140E);
        int edge = Math.round((float)a * 0.55f) << 24 | tint;
        graphics.fill(l, t, l + w, t + 1, edge);
        graphics.fill(l, t + h - 1, l + w, t + h, edge);
        graphics.fill(l, t, l + 1, t + h, edge);
        graphics.fill(l + w - 1, t, l + w, t + h, edge);
        int y = t + 9;
        graphics.drawString(font, this.selected.name(), l + 9, y, a << 24 | (this.selected.owned() ? 0xFFFFFF : 11058352), true);
        y += 14;
        PoseStack pose = graphics.pose();
        y = this.line(graphics, font, pose, l, y, a, Component.translatable((String)"greatsage.detail.tier").getString(), Component.translatable((String)("greatsage.tier." + this.selected.tier())).getString(), tint);
        y = this.line(graphics, font, pose, l, y, a, Component.translatable((String)"greatsage.detail.state").getString(), Component.translatable((String)(this.selected.owned() ? "greatsage.detail.owned" : "greatsage.detail.missing")).getString(), this.selected.owned() ? 14679276 : 10135748);
        if (this.selected.owned()) {
            y = this.line(graphics, font, pose, l, y, a, Component.translatable((String)"greatsage.detail.mastery").getString(), Math.round(this.selected.mastery()) + " / " + this.selected.maxMastery() + "  (" + Math.round(this.selected.ratio() * 100.0f) + "%)", this.selected.ratio() >= 1.0f ? 0xFFFFFF : 9431488);
            int barLeft = l + 9;
            int barWidth = w - 18;
            graphics.fill(barLeft, y + 1, barLeft + barWidth, y + 5, Math.round((float)a * 0.5f) << 24 | 0x2100A);
            graphics.fill(barLeft, y + 1, barLeft + Math.round((float)barWidth * this.selected.ratio()), y + 5, a << 24 | tint);
            y += 12;
        } else {
            y = this.line(graphics, font, pose, l, y, a, Component.translatable((String)"greatsage.detail.maxMastery").getString(), String.valueOf(this.selected.maxMastery()), 9431488);
        }
        y = this.line(graphics, font, pose, l, y, a, Component.translatable((String)"greatsage.detail.cost").getString(), SkillCatalogue.formatCost(this.selected.cost()), 10135748);
        y = this.line(graphics, font, pose, l, y, a, Component.translatable((String)"greatsage.detail.source").getString(), this.selected.id().getNamespace(), 8365459);
        this.line(graphics, font, pose, l, y, a, Component.translatable((String)"greatsage.detail.id").getString(), this.selected.id().getPath(), 7310462);
    }

    private int line(GuiGraphics graphics, Font font, PoseStack pose, int left, int y, int a, String label, String value, int color) {
        pose.pushPose();
        pose.translate((float)left + 9.0f, (float)y, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, label, 0, 0, Math.round((float)a * 0.55f) << 24 | 0x7FA593, false);
        graphics.drawString(font, value, font.width(label) + 6, 0, a << 24 | color, false);
        pose.popPose();
        return y + 11;
    }

    @Nullable
    Entry pick(double mouseX, double mouseY, float left, float top, float width, float height) {
        if (mouseX < (double)left || mouseX > (double)(left + width) || mouseY < (double)top || mouseY > (double)(top + height)) {
            return null;
        }
        List<Entry> rows = this.visible();
        int index = (int)((mouseY - (double)top - (double)this.panY) / 13.0);
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    record Entry(ResourceLocation id, Component name, String tier, boolean owned, double mastery, int maxMastery, double cost) {
        float ratio() {
            return this.maxMastery <= 0 ? 0.0f : Mth.clamp((float)((float)(this.mastery / (double)this.maxMastery)), (float)0.0f, (float)1.0f);
        }
    }
}

