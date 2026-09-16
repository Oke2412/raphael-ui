/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.inventory.InventoryScreen
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.client.AnnouncementOverlay;
import dev.dedek.greatsage.client.SageArchive;
import dev.dedek.greatsage.client.SageFilters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

final class ArchiveView {
    private static final float SCALE = 0.75f;
    private static final float ROW = 14.0f;
    private final Map<String, LivingEntity> models = new HashMap<String, LivingEntity>();
    private final SageFilters filters = new SageFilters(this::invalidate);
    @Nullable
    private SageFilters.Facet carriesFacet;
    @Nullable
    private SageFilters.Facet sourceFacet;
    @Nullable
    private SageFilters.Facet sortFacet;
    @Nullable
    private SageFilters.Facet orderFacet;
    @Nullable
    private List<SageArchive.Sighting> shown;
    private int total;
    @Nullable
    private SageArchive.Sighting selected;
    private String query = "";
    private float panY;
    private float contentHeight;
    private float viewHeight;
    private float detailPan;
    private float detailContent;
    private float detailView;

    ArchiveView() {
    }

    SageFilters filters() {
        return this.filters;
    }

    private void invalidate() {
        this.shown = null;
        this.panY = 0.0f;
    }

    String query() {
        return this.query;
    }

    void setQuery(String value) {
        this.query = value;
        this.invalidate();
    }

    void rebuild() {
        this.filters.clear();
        List<SageArchive.Sighting> sightings = SageArchive.all();
        this.total = sightings.size();
        ArrayList<String> tiers = new ArrayList<String>();
        ArrayList<String> sources = new ArrayList<String>();
        for (SageArchive.Sighting sighting : sightings) {
            for (String tier : sighting.byTier().keySet()) {
                if (tiers.contains(tier)) continue;
                tiers.add(tier);
            }
            String namespace = ArchiveView.namespace(sighting);
            if (sources.contains(namespace)) continue;
            sources.add(namespace);
        }
        tiers.sort(Comparator.naturalOrder());
        sources.sort(Comparator.naturalOrder());
        ArrayList<SageFilters.Option> tierOptions = new ArrayList<SageFilters.Option>();
        for (String tier : tiers) {
            tierOptions.add(new SageFilters.Option(tier, Component.translatable((String)("greatsage.filter.tier." + tier)).getString(), AnnouncementOverlay.tierColor(tier)));
        }
        this.carriesFacet = this.filters.add("greatsage.filter.carries", true, tierOptions);
        ArrayList<SageFilters.Option> arrayList = new ArrayList<SageFilters.Option>();
        for (String namespace : sources) {
            arrayList.add(new SageFilters.Option(namespace, SageFilters.modName(namespace)));
        }
        this.sourceFacet = this.filters.add("greatsage.filter.source", true, arrayList);
        this.sortFacet = this.filters.add("greatsage.filter.sort", false, List.of(ArchiveView.sort("ep"), ArchiveView.sort("name"), ArchiveView.sort("skills"), ArchiveView.sort("samples"), ArchiveView.sort("recent")));
        this.orderFacet = this.filters.add("greatsage.filter.order", false, SageFilters.order(true));
        this.shown = null;
    }

    private static SageFilters.Option sort(String value) {
        return new SageFilters.Option(value, Component.translatable((String)("greatsage.filter.sort." + value)).getString());
    }

    private static String namespace(SageArchive.Sighting sighting) {
        int split = sighting.entityType().indexOf(58);
        return split < 0 ? "minecraft" : sighting.entityType().substring(0, split);
    }

    private List<SageArchive.Sighting> visible() {
        if (this.shown != null) {
            return this.shown;
        }
        String needle = this.query.trim().toLowerCase(Locale.ROOT);
        ArrayList<SageArchive.Sighting> out = new ArrayList<SageArchive.Sighting>();
        for (SageArchive.Sighting sighting : SageArchive.all()) {
            if (!needle.isEmpty() && !sighting.name().toLowerCase(Locale.ROOT).contains(needle) && !sighting.race().toLowerCase(Locale.ROOT).contains(needle) || this.carriesFacet != null && !this.carriesFacet.acceptsAny(sighting.byTier().keySet()) || this.sourceFacet != null && !this.sourceFacet.accepts(ArchiveView.namespace(sighting))) continue;
            out.add(sighting);
        }
        out.sort(this.comparator());
        this.shown = out;
        return out;
    }

    int shownCount() {
        return this.visible().size();
    }

    int total() {
        return this.total;
    }

    private Comparator<SageArchive.Sighting> comparator() {
        Comparator<SageArchive.Sighting> alpha = Comparator.comparing(sighting -> sighting.name().toLowerCase(Locale.ROOT));
        Comparator<SageArchive.Sighting> base = switch (this.sortFacet == null ? "ep" : this.sortFacet.single()) {
            case "name" -> alpha;
            case "skills" -> Comparator.comparingInt(s -> s.skillCount()).thenComparing(alpha);
            case "samples" -> Comparator.comparingInt(s -> s.observations()).thenComparing(alpha);
            case "recent" -> Comparator.comparingLong(s -> s.lastSeen()).thenComparing(alpha);
            default -> Comparator.comparingDouble(s -> s.averageEp()).thenComparing(alpha);
        };
        return this.orderFacet != null && "desc".equals(this.orderFacet.single()) ? base.reversed() : base;
    }

    @Nullable
    SageArchive.Sighting selected() {
        return this.selected;
    }

    void clearSelection() {
        this.selected = null;
    }

    boolean isEmpty() {
        return SageArchive.isEmpty();
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

    void renderList(GuiGraphics graphics, Font font, float left, float top, float width, float height, float alpha, double mouseX, double mouseY) {
        this.viewHeight = height;
        List<SageArchive.Sighting> rows = this.visible();
        this.contentHeight = (float)rows.size() * 14.0f;
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        int first = Math.max(0, (int)(-this.panY / 14.0f) - 1);
        int last = Math.min(rows.size(), first + (int)(height / 14.0f) + 3);
        for (int i = first; i < last; ++i) {
            boolean chosen;
            SageArchive.Sighting sighting = rows.get(i);
            float y = top + this.panY + (float)i * 14.0f;
            if (y < top - 14.0f || y > top + height) continue;
            boolean hover = mouseX >= (double)left && mouseX <= (double)(left + width) && mouseY >= (double)y && mouseY <= (double)(y + 14.0f);
            boolean bl = chosen = sighting == this.selected;
            if (hover || chosen) {
                graphics.fill(Math.round(left), Math.round(y), Math.round(left + width), Math.round(y + 14.0f - 1.0f), Math.round((float)a * (chosen ? 0.3f : 0.18f)) << 24 | 0x1A3A2A);
            }
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(left + 5.0f, y + 3.0f, 0.0f);
            pose.scale(0.75f, 0.75f, 1.0f);
            graphics.drawString(font, sighting.name(), 0, 0, a << 24 | (chosen ? 0xFFFFFF : 14679276), true);
            pose.popPose();
            String right = ArchiveView.format(sighting.averageEp());
            pose.pushPose();
            pose.translate(left + width - 6.0f, y + 3.0f, 0.0f);
            pose.scale(0.75f, 0.75f, 1.0f);
            graphics.drawString(font, right, -font.width(right), 0, Math.round((float)a * 0.85f) << 24 | 0x8FE9C0, true);
            pose.popPose();
        }
    }

    @Nullable
    SageArchive.Sighting pick(double mouseX, double mouseY, float left, float top, float width, float height) {
        if (mouseX < (double)left || mouseX > (double)(left + width) || mouseY < (double)top || mouseY > (double)(top + height)) {
            return null;
        }
        List<SageArchive.Sighting> rows = this.visible();
        int index = (int)((mouseY - (double)top - (double)this.panY) / 14.0);
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    void select(@Nullable SageArchive.Sighting sighting) {
        this.selected = sighting == this.selected ? null : sighting;
        this.detailPan = 0.0f;
    }

    void scroll(double dy) {
        if (this.selected != null) {
            float max = Math.max(0.0f, this.detailContent - this.detailView);
            this.detailPan = Mth.clamp((float)((float)((double)this.detailPan + dy)), (float)(-max), (float)0.0f);
        } else {
            this.pan(dy);
        }
    }

    void renderDetail(GuiGraphics graphics, Font font, float left, float top, float width, float height, float alpha, float time) {
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
        int frame = 66;
        graphics.fill(l, t, l + frame, t + frame, Math.round((float)a * 0.5f) << 24 | 0x2100A);
        int edge = Math.round((float)a * 0.5f) << 24 | 0x8FE9C0;
        graphics.fill(l, t, l + frame, t + 1, edge);
        graphics.fill(l, t + frame - 1, l + frame, t + frame, edge);
        LivingEntity model = this.model(this.selected.entityType());
        if (model != null) {
            int scale = Math.max(4, Math.round(30.0f / Math.max(0.6f, model.getBbHeight())));
            InventoryScreen.renderEntityInInventoryFollowsAngle((GuiGraphics)graphics, (int)l, (int)t, (int)(l + frame), (int)(t + frame), (int)scale, (float)0.15f, (float)(Mth.sin((float)(time / 900.0f)) * 0.9f), (float)0.0f, (LivingEntity)model);
        }
        int textLeft = l + frame + 16;
        graphics.drawString(font, this.selected.name(), textLeft, t + 6, a << 24 | 0xFFFFFF, true);
        int y = t + 21;
        PoseStack pose = graphics.pose();
        if (!this.selected.race().isEmpty()) {
            y = this.line(graphics, font, pose, textLeft, y, a, Component.translatable((String)"greatsage.assess.race").getString(), this.selected.race(), 14679276);
        }
        y = this.line(graphics, font, pose, textLeft, y, a, Component.translatable((String)"greatsage.archive.average").getString(), ArchiveView.format(this.selected.averageEp()), 14679276);
        y = this.line(graphics, font, pose, textLeft, y, a, Component.translatable((String)"greatsage.archive.samples").getString(), String.valueOf(this.selected.observations()), 9431488);
        this.line(graphics, font, pose, textLeft, y, a, Component.translatable((String)"greatsage.assess.health").getString(), String.valueOf(Math.round(this.selected.maxHealth())), 9431488);
        int listTop = t + frame + 12;
        int cursor = 0;
        for (Map.Entry<String, List<String>> group : this.selected.byTier().entrySet()) {
            int tint = AnnouncementOverlay.tierColor(group.getKey());
            String header = Component.translatable((String)("greatsage.tier." + group.getKey())).getString().toUpperCase(Locale.ROOT);
            cursor = this.draw(graphics, font, pose, l, listTop, cursor, a, header, tint, true);
            for (String skill : group.getValue()) {
                cursor = this.draw(graphics, font, pose, l, listTop, cursor, a, skill, 12571849, false);
            }
            cursor += 4;
        }
        this.detailContent = cursor;
        this.detailView = Math.round(height) - (listTop - t);
    }

    private int draw(GuiGraphics graphics, Font font, PoseStack pose, int left, int top, int cursor, int a, String text, int color, boolean header) {
        float y = (float)(top + cursor) + this.detailPan;
        if (y >= (float)top - 12.0f && y <= (float)top + this.detailView) {
            pose.pushPose();
            pose.translate((float)left + (header ? 0.0f : 8.0f), y, 0.0f);
            pose.scale(0.7f, 0.7f, 1.0f);
            graphics.drawString(font, text, 0, 0, Math.round((float)a * (header ? 1.0f : 0.9f)) << 24 | color, true);
            pose.popPose();
        }
        return cursor + (header ? 11 : 9);
    }

    private int line(GuiGraphics graphics, Font font, PoseStack pose, int left, int y, int a, String label, String value, int color) {
        pose.pushPose();
        pose.translate((float)left, (float)y, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, label, 0, 0, Math.round((float)a * 0.55f) << 24 | 0x7FA593, false);
        graphics.drawString(font, value, font.width(label) + 6, 0, a << 24 | color, false);
        pose.popPose();
        return y + 11;
    }

    @Nullable
    private LivingEntity model(String entityType) {
        EntityType type;
        if (this.models.containsKey(entityType)) {
            return this.models.get(entityType);
        }
        LivingEntity built = null;
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation id = ResourceLocation.tryParse((String)entityType);
        if (id != null && minecraft.level != null && (type = (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null)) != null) {
            try {
                Entity entity = type.create((Level)minecraft.level);
                if (entity instanceof LivingEntity) {
                    LivingEntity living;
                    built = living = (LivingEntity)entity;
                }
            }
            catch (RuntimeException failure) {
                built = null;
            }
        }
        this.models.put(entityType, built);
        return built;
    }

    private static String format(double value) {
        if (value >= 1000000.0) {
            return String.format(Locale.ROOT, "%.1fM", value / 1000000.0);
        }
        if (value >= 1000.0) {
            return String.format(Locale.ROOT, "%.1fk", value / 1000.0);
        }
        return String.valueOf(Math.round(value));
    }
}

