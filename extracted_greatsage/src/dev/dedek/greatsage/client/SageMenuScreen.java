/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.Util
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.client.AnalysisClient;
import dev.dedek.greatsage.client.ArchiveView;
import dev.dedek.greatsage.client.BattleView;
import dev.dedek.greatsage.client.GreatSageKeys;
import dev.dedek.greatsage.client.LightFx;
import dev.dedek.greatsage.client.SageArchive;
import dev.dedek.greatsage.client.SageBackdrop;
import dev.dedek.greatsage.client.SageCore;
import dev.dedek.greatsage.client.SageDialogue;
import dev.dedek.greatsage.client.SageFilters;
import dev.dedek.greatsage.client.SageMenuFx;
import dev.dedek.greatsage.client.SettingsView;
import dev.dedek.greatsage.client.SkillCatalogue;
import dev.dedek.greatsage.client.SkillGraph;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class SageMenuScreen
extends Screen {
    private static final float OPEN_MS = 1100.0f;
    private static final float CLOSE_MS = 420.0f;
    private static final float ENTRY_SCALE = 0.85f;
    private static final float DESC_SCALE = 0.75f;
    private static final float TRAVEL_MS = 620.0f;
    private static final Section[] SECTIONS = Section.values();
    private final SkillGraph graph = new SkillGraph();
    private final SkillCatalogue catalogue = new SkillCatalogue();
    private final ArchiveView archive = new ArchiveView();
    private final BattleView battle = new BattleView();
    private final SettingsView settings = new SettingsView();
    private float detailSlide;
    private final long openedAt = Util.getMillis();
    private long closingAt;
    private long lastFrameMs;
    private Stage stage = Stage.MAIN;
    private float travel;
    private float travelTarget;
    @Nullable
    private SkillGraph.Node pending;
    private boolean greeted;
    private boolean manifested;
    private boolean analysisPopup;
    private boolean searchOpen;
    private boolean filterOpen;
    private boolean wasRunning;
    private float frameMs = 16.0f;
    private final float[] sectionHover = new float[SECTIONS.length];
    private float yesHover;
    private float noHover;

    public SageMenuScreen() {
        super((Component)Component.translatable((String)"greatsage.menu.title"));
    }

    protected void init() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.graph.rebuild(this.minecraft.player);
            this.catalogue.rebuild(this.minecraft.player);
            this.battle.rebuild(this.minecraft.player);
        }
        this.settings.rebuild();
        this.archive.rebuild();
        if (!this.manifested) {
            this.manifested = true;
            SageDialogue.play("sfx.open");
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        float global;
        float assembled;
        LightFx.ensureTextures();
        float time = Util.getMillis() - this.openedAt;
        if (this.closingAt == 0L) {
            assembled = Mth.clamp((float)(time / 1100.0f), (float)0.0f, (float)1.0f);
            global = Mth.clamp((float)(time / 260.0f), (float)0.0f, (float)1.0f);
        } else {
            float shut = Mth.clamp((float)((float)(Util.getMillis() - this.closingAt) / 420.0f), (float)0.0f, (float)1.0f);
            assembled = 1.0f - shut;
            global = 1.0f - shut * shut;
            if (shut >= 1.0f && this.minecraft != null) {
                SageDialogue.clear();
                this.minecraft.setScreen(null);
                return;
            }
        }
        if (!this.greeted && this.closingAt == 0L && assembled >= 1.0f) {
            this.greeted = true;
            SageDialogue.greet();
        }
        boolean running = AnalysisClient.running();
        if (this.wasRunning && !running) {
            this.analysisPopup = false;
            if (this.minecraft != null && this.minecraft.player != null) {
                this.graph.rebuild(this.minecraft.player);
            }
        }
        this.wasRunning = running;
        long now = Util.getMillis();
        float frame = this.lastFrameMs == 0L ? 16.0f : Math.min(120.0f, (float)(now - this.lastFrameMs));
        this.lastFrameMs = now;
        this.frameMs = frame;
        this.travel += (this.travelTarget - this.travel) * Mth.clamp((float)(frame / 620.0f * 3.0f), (float)0.0f, (float)1.0f);
        float min = Math.min(this.width, this.height);
        float eased = SageMenuScreen.ease(this.travel);
        float radius = Mth.lerp((float)eased, (float)(min * 0.235f), (float)(min * 0.07f));
        float cx = Mth.lerp((float)eased, (float)((float)this.width / 2.0f), (float)(min * 0.115f));
        float cy = Mth.lerp((float)eased, (float)((float)this.height / 2.0f), (float)(min * 0.115f));
        float reach = Math.max(this.width, this.height);
        SageBackdrop.render(graphics, this.width, this.height, time, global);
        LightFx.beginAdditive();
        SageMenuFx.shell(graphics, cx, cy, radius, reach, time, assembled, global);
        float core = SageMenuFx.coreFade(assembled);
        if (core > 0.0f) {
            SageCore.render(graphics, cx, cy, radius, time, 1.0f, global * core, 9431488, 0.0f);
        }
        LightFx.endAdditive(graphics);
        graphics.flush();
        if (core > 0.0f) {
            float panel = global * core;
            switch (this.stage.ordinal()) {
                case 0: {
                    this.renderMain(graphics, mouseX, mouseY, panel);
                    break;
                }
                case 1: {
                    this.renderAnalyze(graphics, mouseX, mouseY, time, panel);
                    break;
                }
                case 2: {
                    this.renderSkills(graphics, mouseX, mouseY, panel);
                    break;
                }
                case 3: {
                    this.renderArchive(graphics, mouseX, mouseY, time, panel);
                    break;
                }
                case 4: {
                    this.renderBattle(graphics, mouseX, mouseY, panel);
                    break;
                }
                case 5: {
                    this.renderSettings(graphics, mouseX, mouseY, panel);
                }
            }
            this.heading(graphics, panel);
            float bubbleX = Mth.lerp((float)eased, (float)(cx + radius * 0.8f), (float)12.0f);
            float bubbleY = Mth.lerp((float)eased, (float)(cy - radius * 0.25f), (float)(min * 0.205f + 10.0f));
            SageDialogue.render(graphics, Math.min(bubbleX, (float)this.width - 145.0f), bubbleY, panel);
        }
        RenderSystem.disableBlend();
    }

    private void renderMain(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        int i;
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        float time = Util.getMillis() - this.openedAt;
        float cx = (float)this.width / 2.0f;
        float cy = (float)this.height / 2.0f;
        graphics.flush();
        LightFx.beginAdditive();
        for (i = 0; i < SECTIONS.length; ++i) {
            float[] spot = this.orbit(i, time);
            float radius = (float)Math.min(this.width, this.height) * 0.235f;
            float dx = spot[0] - cx;
            float dy = spot[1] - cy;
            float len = Mth.sqrt((float)(dx * dx + dy * dy));
            if (!(len > 1.0f)) continue;
            float fromX = cx + dx / len * radius * 1.05f;
            float fromY = cy + dy / len * radius * 1.05f;
            int steps = 10;
            for (int k = 1; k < steps; ++k) {
                float t = (float)k / (float)steps;
                float ends = Math.min(1.0f, (float)Math.min(k, steps - k) / 3.0f);
                LightFx.glow(graphics, Mth.lerp((float)t, (float)fromX, (float)spot[0]), Mth.lerp((float)t, (float)fromY, (float)spot[1]) + Mth.sin((float)(t * (float)Math.PI)) * 5.0f, 2.6f, 9431488, alpha * 0.34f * ends);
            }
        }
        LightFx.endAdditive(graphics);
        graphics.flush();
        for (i = 0; i < SECTIONS.length; ++i) {
            String label = SECTIONS[i].label();
            float[] box = this.mainBox(i, label);
            this.sectionHover[i] = this.approach(this.sectionHover[i], SageMenuScreen.inside(box, mouseX, mouseY) ? 1.0f : 0.0f);
            this.entryButton(graphics, box, label, this.sectionHover[i], a, alpha);
        }
        int aimed = this.sectionAt(mouseX, mouseY);
        if (aimed >= 0) {
            String desc = SECTIONS[aimed].description();
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate((float)this.width / 2.0f, (float)this.height - 26.0f, 0.0f);
            pose.scale(0.75f, 0.75f, 1.0f);
            graphics.drawString(this.font, desc, -this.font.width(desc) / 2, 0, Math.round((float)a * 0.7f) << 24 | 0xBFD4C9, true);
            pose.popPose();
        }
    }

    private float[] orbit(int index, float time) {
        float min = Math.min(this.width, this.height);
        float step = 360.0f / (float)SECTIONS.length;
        float angle = (-90.0f + (float)index * step + time * 0.0016f) * ((float)Math.PI / 180);
        float ring = min * 0.4f;
        return new float[]{(float)this.width / 2.0f + Mth.cos((float)angle) * ring * 1.25f + Mth.sin((float)(time * 5.0E-4f + (float)index)) * 3.0f, (float)this.height / 2.0f + Mth.sin((float)angle) * ring * 0.78f + Mth.cos((float)(time * 4.0E-4f + (float)index * 1.4f)) * 3.0f};
    }

    private void entryButton(GuiGraphics graphics, float[] box, String label, float hover, int a, float alpha) {
        float lift = SageMenuScreen.ease(hover);
        int color = SageMenuScreen.lerpColor(14679276, 0xFFFFFF, lift);
        int left = Math.round(box[0]);
        int top = Math.round(box[1]);
        int w = Math.round(box[2]);
        int h = Math.round(box[3]);
        graphics.flush();
        LightFx.beginAdditive();
        LightFx.glow(graphics, box[0] + box[2] / 2.0f, box[1] + box[3] / 2.0f, box[2] * (1.0f + lift * 0.3f), box[3] * (1.6f + lift * 1.2f), 14679276, alpha * (0.06f + lift * 0.3f));
        if (lift > 0.02f) {
            float sweep = (float)(Util.getMillis() % 1400L) / 1400.0f;
            LightFx.beam(graphics, box[0] + box[2] * sweep, box[1] + box[3] / 2.0f, box[3] * 1.4f, 10.0f + lift * 12.0f, 90.0f, 0xFFFFFF, alpha * lift * 0.35f);
        }
        LightFx.endAdditive(graphics);
        graphics.flush();
        graphics.fill(left, top, left + w, top + h, Math.round((float)a * (0.45f + lift * 0.3f)) << 24 | 0x4140E);
        int edge = Math.round((float)a * (0.4f + lift * 0.55f)) << 24 | color;
        int reach = Math.round(4.0f + lift * 7.0f);
        int spread = Math.round(lift * 5.0f);
        for (int i = 0; i < 2; ++i) {
            int x = i == 0 ? left - spread : left + w + spread - 1;
            graphics.fill(x, top - 1, x + 1, top + h + 1, edge);
            int from = i == 0 ? x : x - reach + 1;
            graphics.fill(from, top - 1, from + reach, top, edge);
            graphics.fill(from, top + h, from + reach, top + h + 1, edge);
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(box[0] + box[2] / 2.0f, box[1] + box[3] / 2.0f, 0.0f);
        pose.scale(0.85f, 0.85f, 1.0f);
        int n = -this.font.width(label) / 2;
        Objects.requireNonNull(this.font);
        graphics.drawString(this.font, label, n, -9 / 2, a << 24 | color, true);
        pose.popPose();
    }

    private float[] mainBox(int index, String label) {
        float w = (float)this.font.width(label) * 0.85f + 34.0f;
        float h = 17.0f;
        float[] spot = this.orbit(index, Util.getMillis() - this.openedAt);
        return new float[]{spot[0] - w / 2.0f, spot[1] - h / 2.0f, w, h};
    }

    private int sectionAt(double mouseX, double mouseY) {
        for (int i = 0; i < SECTIONS.length; ++i) {
            if (!SageMenuScreen.inside(this.mainBox(i, SECTIONS[i].label()), mouseX, mouseY)) continue;
            return i;
        }
        return -1;
    }

    private void enter(Section section) {
        this.stage = section.stage;
        this.travelTarget = 1.0f;
        this.searchOpen = false;
        this.filterOpen = false;
        this.graph.setQuery("");
        this.catalogue.setQuery("");
        if (section == Section.ANALYZING) {
            SageDialogue.say(this.graph.hasAnalysable() ? "voice.kaiseki_mode" : "voice.kaiseki_fuka");
        } else {
            SageDialogue.say(section.voice);
        }
    }

    private void renderSkills(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        if (this.catalogue.isEmpty()) {
            this.hint(graphics, "greatsage.menu.skills.empty", alpha);
            return;
        }
        float[] panel = this.cataloguePanel();
        float listWidth = this.catalogue.selected() != null ? panel[2] * 0.52f : panel[2];
        this.catalogue.render(graphics, this.font, panel[0], panel[1], listWidth, panel[3], alpha, mouseX, mouseY);
        if (this.catalogue.scrollable()) {
            this.scrollHint(graphics, new float[]{panel[0] + listWidth / 2.0f, panel[1] + panel[3] / 2.0f, listWidth, panel[3]}, alpha);
        }
        if (this.catalogue.shownCount() == 0) {
            this.hint(graphics, "greatsage.filter.none", alpha);
        }
        if (this.catalogue.selected() != null) {
            this.beginOverlay(graphics);
            this.catalogue.renderDetail(graphics, this.font, panel[0] + listWidth + 12.0f, panel[1], panel[2] - listWidth - 12.0f, 122.0f, alpha);
            this.endOverlay(graphics);
        }
        this.tools(graphics, mouseX, mouseY, alpha, this.catalogue.filters(), this.catalogue.shownCount(), this.catalogue.total());
    }

    private void renderArchive(GuiGraphics graphics, int mouseX, int mouseY, float time, float alpha) {
        if (this.archive.isEmpty()) {
            this.hint(graphics, "greatsage.menu.archive.empty", alpha);
            this.magnifier(graphics, mouseX, mouseY, alpha);
            return;
        }
        if (this.archive.selected() == null && this.archive.shownCount() == 0) {
            this.hint(graphics, "greatsage.filter.none", alpha);
        }
        this.detailSlide = this.approach(this.detailSlide, this.archive.selected() != null ? 1.0f : 0.0f);
        float slide = SageMenuScreen.ease(this.detailSlide);
        float[] panel = this.cataloguePanel();
        PoseStack pose = graphics.pose();
        if (slide < 0.995f) {
            pose.pushPose();
            pose.translate(-slide * (panel[2] + 40.0f), 0.0f, 0.0f);
            this.archive.renderList(graphics, this.font, panel[0], panel[1], panel[2], panel[3], alpha * (1.0f - slide), (float)mouseX + slide * (panel[2] + 40.0f), mouseY);
            if (this.archive.scrollable()) {
                this.scrollHint(graphics, new float[]{panel[0] + panel[2] / 2.0f, panel[1] + panel[3] / 2.0f, panel[2], panel[3]}, alpha * (1.0f - slide));
            }
            pose.popPose();
        }
        if (slide > 0.005f) {
            pose.pushPose();
            pose.translate((1.0f - slide) * ((float)this.width - panel[0]), 0.0f, 0.0f);
            this.archive.renderDetail(graphics, this.font, panel[0], panel[1], panel[2], panel[3], alpha * slide, time);
            pose.popPose();
        }
        this.tools(graphics, mouseX, mouseY, alpha, this.archive.filters(), this.archive.shownCount(), this.archive.total());
    }

    private void renderBattle(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        if (this.battle.isEmpty()) {
            this.hint(graphics, "greatsage.menu.battle.empty", alpha);
            return;
        }
        float[] panel = this.cataloguePanel();
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        String header = Component.translatable((String)"greatsage.menu.battle.linked", (Object[])new Object[]{this.battle.linked()}).getString();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(panel[0], panel[1] - 14.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(this.font, header, 0, 0, Math.round((float)a * 0.8f) << 24 | 0x8FE9C0, true);
        pose.popPose();
        this.battle.render(graphics, this.font, panel[0], panel[1], panel[2], panel[3], alpha, mouseX, mouseY);
        if (this.battle.scrollable()) {
            this.scrollHint(graphics, new float[]{panel[0] + panel[2] / 2.0f, panel[1] + panel[3] / 2.0f, panel[2], panel[3]}, alpha);
        }
        this.magnifier(graphics, mouseX, mouseY, alpha);
        if (this.searchOpen) {
            this.searchField(graphics, alpha);
        }
    }

    private void renderSettings(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        if (this.settings.isEmpty()) {
            this.hint(graphics, "greatsage.menu.settings.empty", alpha);
            return;
        }
        float[] panel = this.cataloguePanel();
        this.settings.render(graphics, this.font, panel[0], panel[1], panel[2], panel[3], alpha, mouseX, mouseY);
        if (this.settings.scrollable()) {
            this.scrollHint(graphics, new float[]{panel[0] + panel[2] / 2.0f, panel[1] + panel[3] / 2.0f, panel[2], panel[3]}, alpha);
        }
    }

    private float[] cataloguePanel() {
        float min = Math.min(this.width, this.height);
        float left = min * 0.28f;
        float top = 48.0f;
        return new float[]{left, top, (float)this.width - left - 26.0f, (float)this.height - top - 18.0f};
    }

    private void hint(GuiGraphics graphics, String key, float alpha) {
        String text = Component.translatable((String)key).getString();
        graphics.drawString(this.font, text, (this.width - this.font.width(text)) / 2, this.height / 2, Mth.clamp((int)Math.round(alpha * 200.0f), (int)0, (int)255) << 24 | 0xBFD4C9, true);
    }

    private void renderAnalyze(GuiGraphics graphics, int mouseX, int mouseY, float time, float alpha) {
        if (this.graph.isEmpty()) {
            String text = Component.translatable((String)"greatsage.menu.analyzing.empty").getString();
            graphics.drawString(this.font, text, (this.width - this.font.width(text)) / 2, this.height / 2, Mth.clamp((int)Math.round(alpha * 200.0f), (int)0, (int)255) << 24 | 0xBFD4C9, true);
            return;
        }
        float[] area = this.graphArea();
        this.graph.render(graphics, this.font, area[0], area[1], area[2], area[3], time, alpha, AnalysisClient.skill(), mouseX, mouseY);
        if (this.graph.scrollable()) {
            this.scrollHint(graphics, area, alpha);
        }
        this.magnifier(graphics, mouseX, mouseY, alpha);
        if (this.searchOpen) {
            this.searchField(graphics, alpha);
        }
        if (AnalysisClient.running()) {
            this.gauge(graphics, alpha);
            if (this.analysisPopup) {
                this.analysisCard(graphics, alpha);
            }
        }
        if (this.pending != null) {
            this.confirm(graphics, mouseX, mouseY, alpha);
        }
    }

    private float[] graphArea() {
        float min = Math.min(this.width, this.height);
        float leftEdge = min * 0.26f;
        float topEdge = 46.0f;
        float bottomEdge = (float)this.height - 26.0f;
        return new float[]{(leftEdge + (float)this.width) / 2.0f, (topEdge + bottomEdge) / 2.0f, (float)this.width - leftEdge - 30.0f, bottomEdge - topEdge};
    }

    private float[] searchBox() {
        float[] area = this.graphArea();
        float w = Math.min(220.0f, area[2] * 0.7f);
        return new float[]{area[0] - w / 2.0f, 34.0f, w, 16.0f};
    }

    private void magnifier(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        float[] m = this.magnifierBox();
        boolean hover = SageMenuScreen.over(m, mouseX, mouseY);
        boolean lit = this.searchOpen || !this.query().trim().isEmpty();
        int color = lit || hover ? 0xFFFFFF : 9431488;
        graphics.flush();
        LightFx.beginAdditive();
        float ring = m[2] * 0.58f;
        LightFx.arc(graphics, m[0] - m[2] * 0.14f, m[1] - m[2] * 0.14f, ring, 0.0f, 359.9f, 2.2f, color, alpha * (lit || hover ? 0.95f : 0.55f));
        LightFx.ray(graphics, m[0] + ring * 0.55f, m[1] + ring * 0.55f, m[2] * 0.72f, 2.4f, 45.0f, color, alpha * (lit || hover ? 0.95f : 0.55f));
        if (hover || lit) {
            LightFx.glow(graphics, m[0], m[1], m[2] * 2.2f, color, alpha * 0.16f);
        }
        LightFx.endAdditive(graphics);
        graphics.flush();
    }

    private void tools(GuiGraphics graphics, int mouseX, int mouseY, float alpha, @Nullable SageFilters filters, int shown, int total) {
        this.magnifier(graphics, mouseX, mouseY, alpha);
        if (filters == null) {
            if (this.searchOpen) {
                this.searchField(graphics, alpha);
            }
            return;
        }
        this.funnel(graphics, mouseX, mouseY, alpha, filters);
        if (shown != total && !this.filterOpen) {
            float[] f = this.filterIconBox();
            String text = shown + " / " + total;
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(f[0], f[1] + f[2] + 2.0f, 0.0f);
            pose.scale(0.65f, 0.65f, 1.0f);
            graphics.drawString(this.font, text, -this.font.width(text) / 2, 0, Mth.clamp((int)Math.round(alpha * 190.0f), (int)0, (int)255) << 24 | 0x8FA69A, true);
            pose.popPose();
        }
        if (this.searchOpen) {
            this.searchField(graphics, alpha);
        }
        if (this.filterOpen) {
            this.beginOverlay(graphics);
            float[] box = this.filterPanelBox(filters);
            filters.render(graphics, this.font, box[0], box[1], box[2], box[3], this.filterIconBox()[0], alpha, mouseX, mouseY);
            this.endOverlay(graphics);
        }
    }

    private void funnel(GuiGraphics graphics, int mouseX, int mouseY, float alpha, SageFilters filters) {
        float[] f = this.filterIconBox();
        boolean hover = SageMenuScreen.over(f, mouseX, mouseY);
        boolean lit = this.filterOpen || filters.active();
        int color = lit || hover ? 0xFFFFFF : 9431488;
        float glow = alpha * (lit || hover ? 0.95f : 0.55f);
        graphics.flush();
        LightFx.beginAdditive();
        float w = f[2] * 0.85f;
        float topY = f[1] - w * 0.85f;
        LightFx.beam(graphics, f[0], topY, w * 1.9f, 2.2f, 0.0f, color, glow);
        LightFx.ray(graphics, f[0] - w * 0.95f, topY, w * 1.5f, 2.2f, 52.0f, color, glow);
        LightFx.ray(graphics, f[0] + w * 0.95f, topY, w * 1.5f, 2.2f, 128.0f, color, glow);
        LightFx.ray(graphics, f[0], topY + w * 1.18f, w * 0.5f, 2.2f, 90.0f, color, glow);
        if (hover || lit) {
            LightFx.glow(graphics, f[0], f[1], f[2] * 2.2f, color, alpha * 0.16f);
        }
        LightFx.endAdditive(graphics);
        graphics.flush();
    }

    @Nullable
    private SageFilters filters() {
        return switch (this.stage.ordinal()) {
            case 2 -> this.catalogue.filters();
            case 3 -> this.archive.filters();
            default -> null;
        };
    }

    private float[] filterIconBox() {
        float r = 11.0f;
        return new float[]{(float)this.width - r - 92.0f, r + 18.0f, r};
    }

    private float[] filterPanelBox(SageFilters filters) {
        float[] f = this.filterIconBox();
        float w = Math.min(224.0f, (float)this.width - 16.0f);
        float left = Math.max(8.0f, f[0] + f[2] + 6.0f - w);
        float top = f[1] + f[2] + 8.0f;
        float h = Math.min(filters.contentHeight(this.font, w), (float)this.height - top - 12.0f);
        return new float[]{left, top, w, h};
    }

    private void searchField(GuiGraphics graphics, float alpha) {
        int caret;
        this.beginOverlay(graphics);
        float[] box = this.searchFieldBox();
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        int left = Math.round(box[0]);
        int top = Math.round(box[1]);
        int w = Math.round(box[2]);
        int h = Math.round(box[3]);
        boolean filled = !this.query().isEmpty();
        graphics.fill(left, top, left + w, top + h, a << 24 | 0x4140E);
        int edge = Math.round((float)a * 0.7f) << 24 | 0x8FE9C0;
        graphics.fill(left, top, left + w, top + 1, edge);
        graphics.fill(left, top + h - 1, left + w, top + h, edge);
        graphics.fill(left, top, left + 1, top + h, edge);
        String text = filled ? this.query() : Component.translatable((String)"greatsage.menu.search.placeholder").getString();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float f = (float)left + 6.0f;
        float f2 = (float)top + (float)h / 2.0f;
        Objects.requireNonNull(this.font);
        pose.translate(f, f2 - 9.0f * 0.75f / 2.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(this.font, text, 0, 0, Math.round((float)a * (filled ? 0.95f : 0.45f)) << 24 | (filled ? 14679276 : 8365459), false);
        int n = caret = filled ? this.font.width(text) + 2 : 1;
        if (Util.getMillis() / 500L % 2L == 0L) {
            Objects.requireNonNull(this.font);
            graphics.fill(caret, -1, caret + 1, 9, a << 24 | 0xDFFCEC);
        }
        pose.popPose();
        this.endOverlay(graphics);
    }

    private void scrollHint(GuiGraphics graphics, float[] area, float alpha) {
        int a = Mth.clamp((int)Math.round(alpha * 120.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        int x = Math.round(area[0] + area[2] / 2.0f + 10.0f);
        int top = Math.round(area[1] - area[3] / 2.0f);
        int height = Math.round(area[3]);
        graphics.fill(x, top, x + 1, top + height, Math.round((float)a * 0.4f) << 24 | 0xFFFFFF);
        int thumb = Math.max(18, Math.round((float)height * 0.18f));
        int y = top + Math.round((float)(height - thumb) * (1.0f - this.graph.scrollFraction()));
        graphics.fill(x - 1, y, x + 2, y + thumb, a << 24 | 0x8FE9C0);
    }

    private void beginOverlay(GuiGraphics graphics) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 400.0f);
    }

    private void endOverlay(GuiGraphics graphics) {
        graphics.flush();
        graphics.pose().popPose();
    }

    private float[] gaugeBox() {
        float r = 13.0f;
        return new float[]{(float)this.width - r - 16.0f, r + 16.0f, r};
    }

    private float[] magnifierBox() {
        float r = 11.0f;
        return new float[]{(float)this.width - r - 54.0f, r + 18.0f, r};
    }

    private float[] searchFieldBox() {
        float[] m = this.magnifierBox();
        float w = Math.min(210.0f, (float)this.width * 0.45f);
        return new float[]{m[0] - m[1] - w, m[1] - 8.0f, w, 16.0f};
    }

    private void gauge(GuiGraphics graphics, float alpha) {
        float[] g = this.gaugeBox();
        float progress = AnalysisClient.progress();
        graphics.flush();
        LightFx.beginAdditive();
        LightFx.arc(graphics, g[0], g[1], g[2], 0.0f, 359.9f, 2.6f, 9431488, alpha * 0.3f);
        if (progress > 0.0f) {
            LightFx.arc(graphics, g[0], g[1], g[2], -90.0f, 359.9f * progress, 3.4f, 0xFFFFFF, alpha * 0.95f);
        }
        LightFx.glow(graphics, g[0], g[1], g[2] * 1.1f, 14679276, alpha * 0.18f);
        LightFx.endAdditive(graphics);
        graphics.flush();
        String percent = Math.round(progress * 100.0f) + "%";
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(g[0], g[1], 0.0f);
        pose.scale(0.7f, 0.7f, 1.0f);
        int n = -this.font.width(percent) / 2;
        Objects.requireNonNull(this.font);
        graphics.drawString(this.font, percent, n, -9 / 2, Mth.clamp((int)Math.round(alpha * 220.0f), (int)0, (int)255) << 24 | 0xDFFCEC, true);
        pose.popPose();
    }

    private void analysisCard(GuiGraphics graphics, float alpha) {
        this.beginOverlay(graphics);
        float[] card = this.cardBox();
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        int left = Math.round(card[0]);
        int top = Math.round(card[1]);
        int w = Math.round(card[2]);
        int h = Math.round(card[3]);
        graphics.fill(left, top, left + w, top + h, Math.round((float)a * 0.86f) << 24 | 0x4140E);
        int edge = Math.round((float)a * 0.6f) << 24 | 0x8FE9C0;
        graphics.fill(left, top, left + w, top + 1, edge);
        graphics.fill(left, top + h - 1, left + w, top + h, edge);
        graphics.fill(left, top, left + 1, top + h, edge);
        graphics.fill(left + w - 1, top, left + w, top + h, edge);
        float[] g = this.gaugeBox();
        for (int i = 0; i < 3; ++i) {
            graphics.fill(Math.round(g[0]) - 3 + i, top - 3 + i, Math.round(g[0]) + 3 - i, top - 2 + i, edge);
        }
        MutableComponent name = this.analysedName().copy().withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, (Component)name, left + 8, top + 7, a << 24 | 0xFFFFFF, true);
        boolean stalled = AnalysisClient.stalled();
        String working = stalled ? Component.translatable((String)"greatsage.menu.analyzing.paused").getString() : Component.translatable((String)"greatsage.menu.analyzing.working").getString() + ".".repeat((int)(Util.getMillis() / 420L % 3L) + 1);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((float)(left + 8), (float)(top + 20), 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(this.font, working, 0, 0, Math.round((float)a * 0.75f) << 24 | (stalled ? 15255946 : 12571849), true);
        pose.popPose();
        float[] cross = this.crossBox();
        int cl = Math.round(cross[0]);
        int ct = Math.round(cross[1]);
        int cs = Math.round(cross[2]);
        graphics.fill(cl, ct, cl + cs, ct + cs, Math.round((float)a * 0.35f) << 24 | 0x2A0A0A);
        int red = a << 24 | 0xFF5555;
        for (int i = 2; i < cs - 2; ++i) {
            graphics.fill(cl + i, ct + i, cl + i + 1, ct + i + 1, red);
            graphics.fill(cl + cs - 1 - i, ct + i, cl + cs - i, ct + i + 1, red);
        }
        this.endOverlay(graphics);
    }

    private float[] cardBox() {
        float w = Math.max(150.0f, (float)this.font.width((FormattedText)this.analysedName()) + 62.0f);
        float h = 34.0f;
        float[] g = this.gaugeBox();
        return new float[]{Math.min((float)this.width - w - 8.0f, g[0] - w + g[2] + 6.0f), g[1] + g[2] + 8.0f, w, h};
    }

    private float[] crossBox() {
        float[] card = this.cardBox();
        float size = 13.0f;
        return new float[]{card[0] + card[2] - size - 8.0f, card[1] + 8.0f, size};
    }

    private Component analysedName() {
        ResourceLocation id = AnalysisClient.skill();
        if (id == null || this.minecraft == null || this.minecraft.player == null) {
            return Component.empty();
        }
        return (Component)SkillAPI.getSkillsFrom((LivingEntity)this.minecraft.player).getSkill(id).map(instance -> {
            MutableComponent mutableComponent;
            ManasSkill patt0$temp = instance.getSkill();
            if (patt0$temp instanceof Skill) {
                Skill skill = (Skill)patt0$temp;
                mutableComponent = skill.getName();
            } else {
                mutableComponent = Component.literal((String)id.getPath());
            }
            return mutableComponent;
        }).orElse(Component.literal((String)id.getPath()));
    }

    private void confirm(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
        this.beginOverlay(graphics);
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        int boxWidth = Math.min(300, this.width - 60);
        int boxHeight = 92;
        int left = (this.width - boxWidth) / 2;
        int top = (this.height - boxHeight) / 2;
        graphics.fill(0, 0, this.width, this.height, Math.round((float)a * 0.82f) << 24 | 0x2100A);
        graphics.fill(left, top, left + boxWidth, top + boxHeight, a << 24 | 0x4140E);
        int edge = Math.round((float)a * 0.8f) << 24 | 0x8FE9C0;
        graphics.fill(left, top, left + boxWidth, top + 1, edge);
        graphics.fill(left, top + boxHeight - 1, left + boxWidth, top + boxHeight, edge);
        graphics.fill(left, top, left + 1, top + boxHeight, edge);
        graphics.fill(left + boxWidth - 1, top, left + boxWidth, top + boxHeight, edge);
        String title = Component.translatable((String)"greatsage.menu.confirm.title", (Object[])new Object[]{this.pending.name()}).getString();
        graphics.drawString(this.font, title, left + (boxWidth - this.font.width(title)) / 2, top + 12, a << 24 | 0xFFFFFF, true);
        String warn = Component.translatable((String)"greatsage.menu.confirm.warning").getString();
        for (int i = 0; i < this.font.split((FormattedText)Component.literal((String)warn), boxWidth - 24).size(); ++i) {
            FormattedCharSequence line = (FormattedCharSequence)this.font.split((FormattedText)Component.literal((String)warn), boxWidth - 24).get(i);
            graphics.drawString(this.font, line, left + 12, top + 32 + i * 11, Math.round((float)a * 0.85f) << 24 | 0xE8C98A, false);
        }
        this.yesHover = this.approach(this.yesHover, SageMenuScreen.inside(this.confirmBox(true), mouseX, mouseY) ? 1.0f : 0.0f);
        this.noHover = this.approach(this.noHover, SageMenuScreen.inside(this.confirmBox(false), mouseX, mouseY) ? 1.0f : 0.0f);
        this.button(graphics, this.confirmBox(true), Component.translatable((String)"gui.yes").getString(), 14679276, a, this.yesHover);
        this.button(graphics, this.confirmBox(false), Component.translatable((String)"gui.no").getString(), 14256778, a, this.noHover);
        this.endOverlay(graphics);
    }

    private void button(GuiGraphics graphics, float[] box, String label, int color, int a, float hover) {
        float lift = SageMenuScreen.ease(hover);
        int left = Math.round(box[0]);
        int top = Math.round(box[1]);
        int w = Math.round(box[2]);
        int h = Math.round(box[3]);
        graphics.fill(left, top, left + w, top + h, Math.round((float)a * (0.55f + lift * 0.25f)) << 24 | 0x61C13);
        int wash = Math.round((float)a * lift * 0.28f);
        if (wash > 4) {
            graphics.fill(left, top, left + w, top + h, wash << 24 | color);
        }
        int edge = Math.round((float)a * (0.6f + lift * 0.4f)) << 24 | color;
        graphics.fill(left, top, left + w, top + 1, edge);
        graphics.fill(left, top + h - 1, left + w, top + h, edge);
        int side = Math.round(lift * (float)h);
        if (side > 0) {
            graphics.fill(left, top, left + 1, top + side, edge);
            graphics.fill(left + w - 1, top + h - side, left + w, top + h, edge);
        }
        int n = left + (w - this.font.width(label)) / 2;
        Objects.requireNonNull(this.font);
        graphics.drawString(this.font, label, n, top + (h - 9) / 2, a << 24 | SageMenuScreen.lerpColor(color, 0xFFFFFF, lift * 0.6f), true);
    }

    private float[] confirmBox(boolean yes) {
        int boxWidth = Math.min(300, this.width - 60);
        int left = (this.width - boxWidth) / 2;
        int top = (this.height - 92) / 2;
        float w = 74.0f;
        return new float[]{yes ? (float)left + (float)boxWidth / 2.0f - w - 8.0f : (float)left + (float)boxWidth / 2.0f + 8.0f, (float)top + 62.0f, w, 18.0f};
    }

    private void heading(GuiGraphics graphics, float alpha) {
        String text = this.title.getString().toUpperCase(Locale.ROOT);
        int a = Mth.clamp((int)Math.round(alpha * 0.5f * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        float spacing = 2.4f;
        float total = -spacing;
        for (int i = 0; i < text.length(); ++i) {
            total += (float)this.font.width(String.valueOf(text.charAt(i))) + spacing;
        }
        float cursor = (float)this.width / 2.0f - total / 2.0f;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        for (int i = 0; i < text.length(); ++i) {
            String glyph = String.valueOf(text.charAt(i));
            graphics.drawString(this.font, glyph, Math.round(cursor), 18, a << 24 | 0x8FE9C0, true);
            cursor += (float)this.font.width(glyph) + spacing;
        }
        pose.popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Object picked;
        SageFilters filters;
        float[] panel;
        if (button == 1 && this.closingAt == 0L && this.pending == null && this.stage == Stage.SETTINGS && this.settings.click(mouseX, mouseY, (panel = this.cataloguePanel())[0], panel[1], panel[2], panel[3], true)) {
            SageDialogue.say("voice.ryoukai");
            return true;
        }
        if (button == 1 && this.closingAt == 0L && this.pending == null && (filters = this.filters()) != null && SageMenuScreen.over(this.filterIconBox(), mouseX, mouseY)) {
            filters.reset();
            return true;
        }
        if (button != 0 || this.closingAt != 0L) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.pending != null) {
            if (SageMenuScreen.inside(this.confirmBox(true), mouseX, mouseY)) {
                AnalysisClient.request(this.pending.id());
                this.pending = null;
            } else if (SageMenuScreen.inside(this.confirmBox(false), mouseX, mouseY)) {
                this.pending = null;
            }
            return true;
        }
        if (this.stage != Stage.MAIN && this.overSage(mouseX, mouseY)) {
            if (this.filterOpen) {
                this.filterOpen = false;
                return true;
            }
            if (this.stage == Stage.ARCHIVE && this.archive.selected() != null) {
                this.archive.clearSelection();
                return true;
            }
            if (this.stage == Stage.SKILLS && this.catalogue.selected() != null) {
                this.catalogue.clearSelection();
                return true;
            }
            if (this.stage == Stage.ANALYZE && this.graph.expanded() != null) {
                this.graph.collapse();
                return true;
            }
            this.stage = Stage.MAIN;
            this.travelTarget = 0.0f;
            this.searchOpen = false;
            this.filterOpen = false;
            this.graph.collapse();
            this.catalogue.clearSelection();
            this.archive.clearSelection();
            return true;
        }
        if (this.stage == Stage.MAIN) {
            int aimed = this.sectionAt(mouseX, mouseY);
            if (aimed >= 0) {
                this.enter(SECTIONS[aimed]);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.stage == Stage.SETTINGS) {
            panel = this.cataloguePanel();
            if (this.settings.click(mouseX, mouseY, panel[0], panel[1], panel[2], panel[3], false)) {
                SageDialogue.say("voice.ryoukai");
            }
            return true;
        }
        if (this.stage == Stage.BATTLE) {
            if (SageMenuScreen.over(this.magnifierBox(), mouseX, mouseY)) {
                boolean bl = this.searchOpen = !this.searchOpen;
                if (!this.searchOpen) {
                    this.battle.setQuery("");
                }
                return true;
            }
            panel = this.cataloguePanel();
            Boolean linked = this.battle.click(mouseX, mouseY, panel[0], panel[1], panel[2], panel[3]);
            if (linked != null) {
                SageDialogue.reply(List.of("voice.ryo", linked != false ? "reply.rendou_kanryou" : "reply.rendou_kaijo"), (Component)Component.translatable((String)"greatsage.menu.battle.linked", (Object[])new Object[]{this.battle.linked()}));
            }
            return true;
        }
        if (this.stage == Stage.ARCHIVE) {
            if (this.filterClick(mouseX, mouseY)) {
                return true;
            }
            if (SageMenuScreen.over(this.magnifierBox(), mouseX, mouseY)) {
                this.searchOpen = !this.searchOpen;
                this.filterOpen = false;
                if (!this.searchOpen) {
                    this.archive.setQuery("");
                }
                return true;
            }
            if (this.archive.selected() != null) {
                return true;
            }
            panel = this.cataloguePanel();
            SageArchive.Sighting hit = this.archive.pick(mouseX, mouseY, panel[0], panel[1], panel[2], panel[3]);
            if (hit != null) {
                this.archive.select(hit);
            }
            return true;
        }
        if (this.stage == Stage.SKILLS) {
            float listWidth;
            if (this.filterClick(mouseX, mouseY)) {
                return true;
            }
            if (SageMenuScreen.over(this.magnifierBox(), mouseX, mouseY)) {
                this.searchOpen = !this.searchOpen;
                this.filterOpen = false;
                if (!this.searchOpen) {
                    this.catalogue.setQuery("");
                }
                return true;
            }
            panel = this.cataloguePanel();
            SkillCatalogue.Entry hit = this.catalogue.pick(mouseX, mouseY, panel[0], panel[1], listWidth = this.catalogue.selected() != null ? panel[2] * 0.52f : panel[2], panel[3]);
            if (hit != null) {
                boolean opening = !hit.equals(this.catalogue.selected());
                this.catalogue.select(hit);
                if (opening && !hit.owned()) {
                    SageDialogue.say("voice.mishutoku");
                }
            }
            return true;
        }
        if (SageMenuScreen.over(this.magnifierBox(), mouseX, mouseY)) {
            this.searchOpen = !this.searchOpen;
            this.analysisPopup = false;
            if (!this.searchOpen) {
                this.graph.setQuery("");
            }
            return true;
        }
        if (this.searchOpen && SageMenuScreen.inside(this.searchFieldBox(), mouseX, mouseY)) {
            return true;
        }
        if (AnalysisClient.running()) {
            float[] g = this.gaugeBox();
            if (SageMenuScreen.over(g, mouseX, mouseY)) {
                this.analysisPopup = !this.analysisPopup;
                this.searchOpen = false;
                return true;
            }
            if (this.analysisPopup) {
                float[] cross = this.crossBox();
                if (mouseX >= (double)cross[0] && mouseX <= (double)(cross[0] + cross[2]) && mouseY >= (double)cross[1] && mouseY <= (double)(cross[1] + cross[2])) {
                    AnalysisClient.requestCancel();
                    this.analysisPopup = false;
                    return true;
                }
                if (SageMenuScreen.inside(this.cardBox(), mouseX, mouseY)) {
                    return true;
                }
                this.analysisPopup = false;
            }
        }
        if ((picked = this.graph.pick(mouseX, mouseY)) instanceof String) {
            String tier = (String)picked;
            this.graph.expand(tier);
            if (this.graph.expanded() != null && !this.graph.hasAnalysable(tier)) {
                SageDialogue.say("voice.kaiseki_fuka");
            }
            return true;
        }
        if (picked instanceof SkillGraph.Node) {
            SkillGraph.Node node = (SkillGraph.Node)picked;
            if (node.analysable()) {
                this.pending = node;
                SageDialogue.say("voice.kaiseki_kakunin");
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean filterClick(double mouseX, double mouseY) {
        SageFilters filters = this.filters();
        if (filters == null) {
            return false;
        }
        if (SageMenuScreen.over(this.filterIconBox(), mouseX, mouseY)) {
            this.filterOpen = !this.filterOpen;
            this.searchOpen = false;
            return true;
        }
        if (!this.filterOpen) {
            return false;
        }
        float[] box = this.filterPanelBox(filters);
        if (filters.click(this.font, box[0], box[1], box[2], box[3], mouseX, mouseY)) {
            return true;
        }
        this.filterOpen = false;
        return true;
    }

    private boolean overSage(double mouseX, double mouseY) {
        float radius;
        float cy;
        float dy;
        float min = Math.min(this.width, this.height);
        float cx = Mth.lerp((float)SageMenuScreen.ease(this.travel), (float)((float)this.width / 2.0f), (float)(min * 0.115f));
        float dx = (float)mouseX - cx;
        return dx * dx + (dy = (float)mouseY - (cy = Mth.lerp((float)SageMenuScreen.ease(this.travel), (float)((float)this.height / 2.0f), (float)(min * 0.115f)))) * dy <= (radius = Mth.lerp((float)SageMenuScreen.ease(this.travel), (float)(min * 0.235f), (float)(min * 0.07f)) * 1.25f) * radius;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.pending == null) {
            switch (this.stage.ordinal()) {
                case 1: {
                    this.graph.pan(dragY);
                    break;
                }
                case 2: {
                    this.catalogue.pan(dragY);
                    break;
                }
                case 3: {
                    this.archive.scroll(dragY);
                    break;
                }
                case 4: {
                    this.battle.pan(dragY);
                    break;
                }
                case 5: {
                    this.settings.pan(dragY);
                    break;
                }
                default: {
                    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float[] box;
        SageFilters filters = this.filters();
        if (this.filterOpen && filters != null && SageMenuScreen.inside(box = this.filterPanelBox(filters), mouseX, mouseY)) {
            filters.pan(scrollY * 16.0, this.font, box[2], box[3]);
            return true;
        }
        if (this.pending == null) {
            switch (this.stage.ordinal()) {
                case 1: {
                    this.graph.pan(scrollY * 18.0);
                    break;
                }
                case 2: {
                    this.catalogue.pan(scrollY * 22.0);
                    break;
                }
                case 3: {
                    this.archive.scroll(scrollY * 22.0);
                    break;
                }
                case 4: {
                    this.battle.pan(scrollY * 22.0);
                    break;
                }
                case 5: {
                    this.settings.pan(scrollY * 22.0);
                    break;
                }
                default: {
                    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchOpen && this.pending == null && codePoint >= ' ') {
            this.setQuery(this.query() + codePoint);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (this.closingAt != 0L) {
            return true;
        }
        if (GreatSageKeys.OPEN_MENU.matches(key, scanCode) && !this.searchOpen) {
            this.onClose();
            return true;
        }
        if (key == 259 && this.searchOpen && this.pending == null && !this.query().isEmpty()) {
            this.setQuery(this.query().substring(0, this.query().length() - 1));
            return true;
        }
        if (key == 256) {
            if (this.pending != null) {
                this.pending = null;
            } else if (this.filterOpen) {
                this.filterOpen = false;
            } else if (this.searchOpen) {
                this.searchOpen = false;
                this.setQuery("");
            } else if (this.stage == Stage.SKILLS && this.catalogue.selected() != null) {
                this.catalogue.clearSelection();
            } else if (this.stage == Stage.ARCHIVE && this.archive.selected() != null) {
                this.archive.clearSelection();
            } else if (this.stage == Stage.ANALYZE && this.graph.expanded() != null) {
                this.graph.collapse();
            } else if (this.stage != Stage.MAIN) {
                this.stage = Stage.MAIN;
                this.travelTarget = 0.0f;
            } else {
                this.onClose();
            }
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    public void onClose() {
        if (this.closingAt == 0L) {
            this.closingAt = Util.getMillis();
            SageDialogue.clear();
            SageDialogue.play("sfx.close");
        }
    }

    private String query() {
        return switch (this.stage.ordinal()) {
            case 2 -> this.catalogue.query();
            case 3 -> this.archive.query();
            case 4 -> this.battle.query();
            default -> this.graph.query();
        };
    }

    private void setQuery(String value) {
        switch (this.stage.ordinal()) {
            case 2: {
                this.catalogue.setQuery(value);
                break;
            }
            case 3: {
                this.archive.setQuery(value);
                break;
            }
            case 4: {
                this.battle.setQuery(value);
                break;
            }
            default: {
                this.graph.setQuery(value);
            }
        }
    }

    private float approach(float value, float target) {
        return value + (target - value) * Mth.clamp((float)(this.frameMs / 150.0f), (float)0.0f, (float)1.0f);
    }

    private static int lerpColor(int from, int to, float t) {
        float k = Mth.clamp((float)t, (float)0.0f, (float)1.0f);
        int r = Math.round(Mth.lerp((float)k, (float)(from >> 16 & 0xFF), (float)(to >> 16 & 0xFF)));
        int g = Math.round(Mth.lerp((float)k, (float)(from >> 8 & 0xFF), (float)(to >> 8 & 0xFF)));
        int b = Math.round(Mth.lerp((float)k, (float)(from & 0xFF), (float)(to & 0xFF)));
        return r << 16 | g << 8 | b;
    }

    private static boolean over(float[] circle, double x, double y) {
        double dx = x - (double)circle[0];
        double dy = y - (double)circle[1];
        double reach = (double)circle[2] + 5.0;
        return dx * dx + dy * dy <= reach * reach;
    }

    private static boolean inside(float[] box, double x, double y) {
        return x >= (double)box[0] && x <= (double)(box[0] + box[2]) && y >= (double)box[1] && y <= (double)(box[1] + box[3]);
    }

    private static float ease(float t) {
        float clamped = Mth.clamp((float)t, (float)0.0f, (float)1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    private static enum Stage {
        MAIN,
        ANALYZE,
        SKILLS,
        ARCHIVE,
        BATTLE,
        SETTINGS;

    }

    private static enum Section {
        ANALYZING("analyzing", Stage.ANALYZE, "voice.kaiseki_mode"),
        SKILLS("skills", Stage.SKILLS, "voice.nouryoku_ichiran"),
        ARCHIVE("archive", Stage.ARCHIVE, "voice.nouryoku_ichiran"),
        BATTLE("battle", Stage.BATTLE, "voice.sentou_mode"),
        SETTINGS("settings", Stage.SETTINGS, "voice.nouryoku_ichiran");

        private final String id;
        private final Stage stage;
        private final String voice;

        private Section(String id, Stage stage, String voice) {
            this.id = id;
            this.stage = stage;
            this.voice = voice;
        }

        String label() {
            return Component.translatable((String)("greatsage.menu." + this.id)).getString().toUpperCase(Locale.ROOT);
        }

        String description() {
            return Component.translatable((String)("greatsage.menu." + this.id + ".desc")).getString();
        }
    }
}

