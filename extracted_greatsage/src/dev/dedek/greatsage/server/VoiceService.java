/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.race.api.ManasRace
 *  io.github.manasmods.manascore.race.api.ManasRaceInstance
 *  io.github.manasmods.manascore.race.api.RaceAPI
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  io.github.manasmods.tensura.ability.subclass.ISpatialStorage
 *  io.github.manasmods.tensura.menu.container.SpatialStorageContainer
 *  io.github.manasmods.tensura.storage.TensuraStorages
 *  io.github.manasmods.tensura.storage.chunk.ChunkStorage
 *  io.github.manasmods.tensura.storage.ep.IExistence
 *  io.github.manasmods.tensura.util.EnergyHelper
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.stats.Stats
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.EquipmentSlot$Type
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.monster.Enemy
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.item.ArmorItem
 *  net.minecraft.world.item.AxeItem
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.BowItem
 *  net.minecraft.world.item.CrossbowItem
 *  net.minecraft.world.item.DiggerItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.PickaxeItem
 *  net.minecraft.world.item.PotionItem
 *  net.minecraft.world.item.ShieldItem
 *  net.minecraft.world.item.SwordItem
 *  net.minecraft.world.item.alchemy.PotionContents
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.item.crafting.RecipeHolder
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package dev.dedek.greatsage.server;

import dev.dedek.greatsage.GreatSageGameRules;
import dev.dedek.greatsage.network.VoiceReplyPayload;
import dev.dedek.greatsage.server.AnalysisManager;
import dev.dedek.greatsage.server.BattleMode;
import dev.dedek.greatsage.voice.NumberSpeech;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.subclass.ISpatialStorage;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.chunk.ChunkStorage;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoiceService {
    private static final Logger LOG = LoggerFactory.getLogger((String)"greatsage/voice");
    private static final double NEARBY = 24.0;
    private static final int SEARCH_REACH = 16;
    private static final int HOTBAR = 9;

    private VoiceService() {
    }

    public static void handle(ServerPlayer player, String command, String argument, int targetId) {
        if (!GreatSageGameRules.carries((LivingEntity)player)) {
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new VoiceReplyPayload(List.of("voice.hi", "reply.kengen_nashi"), "greatsage.voice.kengen_nashi", List.of()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            return;
        }
        Answer answer = new Answer();
        try {
            VoiceService.answer(player, command, argument, targetId, answer);
        }
        catch (Throwable failure) {
            LOG.warn("[greatsage] la commande {} a echoue : {}", (Object)command, (Object)failure.toString());
            answer = new Answer();
            answer.say("voice.hi").say("reply.machine_error").text("greatsage.voice.machine_error");
        }
        if (answer.clips.isEmpty()) {
            answer.say("voice.hi").say("reply.jouhou_nashi").text("greatsage.voice.jouhou_nashi");
        }
        LOG.info("[greatsage] commande {} -> {} {}", new Object[]{command, answer.subtitle.isEmpty() ? "aucun sous-titre" : answer.subtitle, answer.args.isEmpty() ? "" : answer.args});
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new VoiceReplyPayload(List.copyOf(answer.clips), answer.subtitle, List.copyOf(answer.args)), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private static void answer(ServerPlayer player, String command, String argument, int targetId, Answer out) {
        switch (command) {
            case "my_ep": 
            case "my_power": {
                out.say("voice.koku").value("reply.sonzaichi", EnergyHelper.getMaxEP((LivingEntity)player)).text("greatsage.voice.sonzaichi");
                break;
            }
            case "my_magicules": {
                out.say("voice.koku").value("reply.masoryou", VoiceService.magicule(player)).text("greatsage.voice.masoryou");
                break;
            }
            case "magicule_check": {
                boolean enough = VoiceService.magicule(player) > VoiceService.maxMagicule(player) * 0.25;
                out.say(enough ? "voice.ze" : "voice.hi").say(enough ? "reply.maso_juubun" : "reply.maso_fusoku").text(enough ? "greatsage.voice.maso_juubun" : "greatsage.voice.maso_fusoku");
                break;
            }
            case "my_health": {
                out.say("voice.koku").value("reply.tairyoku", player.getHealth()).text("greatsage.voice.tairyoku");
                break;
            }
            case "injured": {
                boolean hurt = player.getHealth() < player.getMaxHealth() - 0.5f;
                out.say(hurt ? "voice.ze" : "voice.hi").say(hurt ? "reply.fushou_ari" : "reply.futsuu").text(hurt ? "greatsage.voice.fushou_ari" : "greatsage.voice.futsuu");
                break;
            }
            case "my_race": {
                ResourceLocation race = VoiceService.raceId((LivingEntity)player);
                out.say("voice.koku").named("reply.shuzoku", race == null ? null : "race." + race.getPath(), race == null ? "?" : race.getPath()).say("num.desu").text("greatsage.voice.shuzoku");
                break;
            }
            case "hungry": {
                boolean hungry = player.getFoodData().getFoodLevel() < 12;
                out.say(hungry ? "voice.ze" : "voice.hi").say(hungry ? "reply.kuufuku" : "reply.futsuu").text(hungry ? "greatsage.voice.kuufuku" : "greatsage.voice.futsuu");
                break;
            }
            case "burning": {
                VoiceService.yesNo(out, player.isOnFire());
                break;
            }
            case "air": {
                out.say("voice.koku").value("reply.sanso", (double)Math.max(0, player.getAirSupply()) / 20.0).text("greatsage.voice.sanso");
                break;
            }
            case "holding": {
                out.say("voice.koku").say("reply.shoji").say("num.desu").arg(player.getMainHandItem().getHoverName().getString()).text("greatsage.voice.shoji");
                break;
            }
            case "durability": 
            case "tool_breaking": {
                boolean critical;
                ItemStack stack = player.getMainHandItem();
                if (!stack.isDamageableItem()) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                int left = stack.getMaxDamage() - stack.getDamageValue();
                boolean bl = critical = (double)left < (double)stack.getMaxDamage() * 0.1;
                if ("tool_breaking".equals(command) && critical) {
                    out.say("voice.koku").say("reply.taikyuu_genkai").text("greatsage.voice.taikyuu_genkai");
                    break;
                }
                out.say("voice.koku").value("reply.taikyuu", left).text("greatsage.voice.taikyuu");
                break;
            }
            case "armour": {
                out.say("voice.koku").value("reply.bougyo", player.getArmorValue()).text("greatsage.voice.bougyo");
                break;
            }
            case "deaths": {
                out.say("voice.koku").value("reply.shibou_kaisuu", player.getStats().getValue(Stats.CUSTOM.get((Object)Stats.DEATHS))).text("greatsage.voice.shibou_kaisuu");
                break;
            }
            case "in_danger": 
            case "area_danger": {
                LivingEntity worst = VoiceService.strongestNearby(player);
                boolean danger = worst != null && EnergyHelper.getMaxEP((LivingEntity)worst) > EnergyHelper.getMaxEP((LivingEntity)player);
                out.say(danger ? "voice.koku" : "voice.hi").say(danger ? "reply.kiken_ari" : "reply.kiken_nashi").text(danger ? "greatsage.voice.kiken_ari" : "greatsage.voice.kiken_nashi");
                break;
            }
            case "skill_count": {
                out.say("voice.koku").value("reply.hoyuu_suu", VoiceService.ownedSkills(player).size()).text("greatsage.voice.hoyuu_suu");
                break;
            }
            case "mastered_count": {
                out.say("voice.koku").value("reply.shuutoku_suu", VoiceService.ownedSkills(player).stream().filter(instance -> instance.isMastered((LivingEntity)player)).count()).text("greatsage.voice.shuutoku_suu");
                break;
            }
            case "total_skills": {
                int total = 0;
                for (ManasSkill entry : SkillAPI.getSkillRegistry()) {
                    if (!(entry instanceof Skill)) continue;
                    ++total;
                }
                out.say("voice.koku").value("reply.souhoyuu_suu", total).text("greatsage.voice.souhoyuu_suu");
                break;
            }
            case "missing_skills": {
                int total = 0;
                for (ManasSkill entry : SkillAPI.getSkillRegistry()) {
                    if (!(entry instanceof Skill)) continue;
                    ++total;
                }
                out.say("voice.koku").value("reply.mishutoku_suu", total - VoiceService.ownedSkills(player).size()).text("greatsage.voice.mishutoku_suu");
                break;
            }
            case "strongest_skill": 
            case "rarest_skill": 
            case "weakest_skill": {
                ManasSkillInstance best = VoiceService.rarest(player);
                if (best == null) break;
                ResourceLocation id = best.getSkillId();
                out.say("voice.koku").named("rarest_skill".equals(command) ? "reply.kishou_skill" : "reply.saikyou_skill", VoiceService.clipFor(id), VoiceService.name(best)).say("num.desu").text("greatsage.voice.saikyou_skill");
                break;
            }
            case "closest_mastery": {
                ManasSkillInstance closest = null;
                double bestRatio = -1.0;
                for (ManasSkillInstance instance2 : VoiceService.ownedSkills(player)) {
                    ManasSkill manasSkill;
                    if (instance2.isMastered((LivingEntity)player) || !((manasSkill = instance2.getSkill()) instanceof Skill)) continue;
                    Skill skill = (Skill)manasSkill;
                    double d = skill.getMaxMastery() <= 0 ? 0.0 : instance2.getMastery() / (double)skill.getMaxMastery();
                    double ratio = d;
                    if (!(ratio > bestRatio)) continue;
                    bestRatio = ratio;
                    closest = instance2;
                }
                if (closest == null) break;
                out.say("voice.koku").named("reply.mousugu", VoiceService.clipFor(closest.getSkillId()), VoiceService.name(closest)).text("greatsage.voice.mousugu");
                break;
            }
            case "have_skill": 
            case "skill_mastered": {
                ManasSkillInstance instance3 = VoiceService.find(player, argument);
                boolean yes = instance3 != null && (!"skill_mastered".equals(command) || instance3.isMastered((LivingEntity)player));
                out.say(yes ? "voice.ze" : "voice.hi").say(yes ? "reply.shutoku_zumi" : "voice.mishutoku").arg(argument).text(yes ? "greatsage.voice.shutoku_zumi" : "greatsage.voice.mishutoku");
                break;
            }
            case "skill_mastery": {
                ManasSkill entry;
                ManasSkillInstance instance4 = VoiceService.find(player, argument);
                if (instance4 == null || !((entry = instance4.getSkill()) instanceof Skill)) break;
                Skill skill = (Skill)entry;
                double ratio = skill.getMaxMastery() <= 0 ? 0.0 : instance4.getMastery() * 100.0 / (double)skill.getMaxMastery();
                out.say("voice.koku").arg(VoiceService.name(instance4)).percent("reply.shuujukudo", ratio).text("greatsage.voice.shuujukudo");
                break;
            }
            case "skill_cost": {
                double mp;
                Skill skill = VoiceService.lookup(argument);
                if (skill == null) break;
                ManasSkillInstance held = VoiceService.find(player, argument);
                double d = mp = held != null ? skill.getAcquiringMagiculeCost(held) : skill.getCachedAcquiringMpCost();
                if (mp <= 0.0) {
                    mp = skill.getDefaultAcquiringMagiculeCost();
                }
                out.say("voice.koku").arg(skill.getName().getString()).value("reply.hitsuyou_maso", mp).text("greatsage.voice.hitsuyou_maso");
                break;
            }
            case "skill_source": {
                Skill skill = VoiceService.lookup(argument);
                if (skill == null || skill.getRegistryName() == null) break;
                out.say("voice.koku").say("reply.shutten").say("num.desu").arg(skill.getRegistryName().getNamespace()).text("greatsage.voice.shutten");
                break;
            }
            case "analyse": {
                Skill skill = VoiceService.lookup(argument);
                if (skill == null || skill.getRegistryName() == null) break;
                if (AnalysisManager.analysedSkill(player) != null) {
                    out.say("voice.hi").say("reply.jikkou_chuu").text("greatsage.voice.jikkou_chuu");
                    break;
                }
                AnalysisManager.start(player, skill.getRegistryName().toString());
                out.say("voice.ryo").say("reply.kaiseki_kaishi").arg(skill.getName().getString()).text("greatsage.voice.kaiseki_kaishi");
                break;
            }
            case "analysis_stop": {
                AnalysisManager.cancel(player);
                out.say("voice.ryo").say("reply.kaiseki_chuudan").text("greatsage.voice.kaiseki_chuudan");
                break;
            }
            case "what_is_this": 
            case "target_race": {
                LivingEntity target2 = VoiceService.target(player, targetId);
                if (target2 == null) {
                    out.say("voice.hi").say("reply.taishou_nashi").text("greatsage.voice.taishou_nashi");
                    break;
                }
                ResourceLocation race = VoiceService.raceId(target2);
                out.say("voice.koku").named("reply.taishou", race == null ? null : "race." + race.getPath(), target2.getType().getDescription().getString()).say("num.desu").text("greatsage.voice.taishou");
                break;
            }
            case "target_power": {
                VoiceService.withTarget(player, targetId, out, target -> out.say("voice.koku").value("reply.taishou_sonzaichi", EnergyHelper.getMaxEP((LivingEntity)target)).text("greatsage.voice.taishou_sonzaichi"));
                break;
            }
            case "target_health": 
            case "enemy_health": {
                VoiceService.withTarget(player, targetId, out, target -> out.say("voice.koku").value("reply.taishou_tairyoku", target.getHealth()).text("greatsage.voice.taishou_tairyoku"));
                break;
            }
            case "target_distance": {
                VoiceService.withTarget(player, targetId, out, target -> out.say("voice.koku").value("reply.kyori", player.distanceTo((Entity)target)).text("greatsage.voice.kyori"));
                break;
            }
            case "target_hostile": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    boolean hostile = target instanceof Enemy;
                    out.say(hostile ? "voice.ze" : "voice.hi").say(hostile ? "reply.tekisei_ari" : "reply.tekisei_nashi").text(hostile ? "greatsage.voice.tekisei_ari" : "greatsage.voice.tekisei_nashi");
                });
                break;
            }
            case "about_to_die": {
                VoiceService.withTarget(player, targetId, out, target -> VoiceService.yesNo(out, target.getHealth() < target.getMaxHealth() * 0.15f));
                break;
            }
            case "compare_target": 
            case "can_i_win": 
            case "should_fight": 
            case "overleveled": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    double theirs;
                    double mine = EnergyHelper.getMaxEP((LivingEntity)player);
                    boolean winning = mine >= (theirs = EnergyHelper.getMaxEP((LivingEntity)target));
                    out.say(winning ? "voice.ze" : "voice.koku").say(winning ? "reply.shouri_kanou" : "reply.tettai_suishou").text(winning ? "greatsage.voice.shouri_kanou" : "greatsage.voice.tettai_suishou");
                });
                break;
            }
            case "survival_odds": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    double mine = EnergyHelper.getMaxEP((LivingEntity)player);
                    double theirs = Math.max(1.0, EnergyHelper.getMaxEP((LivingEntity)target));
                    out.say("voice.koku").percent("voice.seizon_kakuritsu", Math.min(99.0, mine * 100.0 / (mine + theirs))).text("greatsage.voice.seizon_kakuritsu");
                });
                break;
            }
            case "strongest_nearby": 
            case "pick_target": {
                LivingEntity worst = VoiceService.strongestNearby(player);
                if (worst == null) {
                    out.say("voice.hi").say("reply.kiken_nashi").text("greatsage.voice.kiken_nashi");
                    break;
                }
                ResourceLocation race = VoiceService.raceId(worst);
                out.say("voice.koku").named("reply.saikyou_taishou", race == null ? null : "race." + race.getPath(), worst.getType().getDescription().getString()).say("num.desu").text("greatsage.voice.saikyou_taishou");
                break;
            }
            case "enemy_count": 
            case "target_count": {
                out.say("voice.koku").value("reply.taisuu", VoiceService.nearby(player).size()).text("greatsage.voice.taisuu");
                break;
            }
            case "behind_me": {
                LivingEntity back = VoiceService.behind(player);
                out.say(back == null ? "voice.hi" : "voice.koku").say(back == null ? "reply.kiken_nashi" : "reply.haigo_keikoku").text(back == null ? "greatsage.voice.kiken_nashi" : "greatsage.voice.haigo_keikoku");
                break;
            }
            case "target_name": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    boolean named = target.hasCustomName();
                    out.say("voice.koku").say(named ? "reply.meishou" : "reply.gaitou_nashi").arg(named ? target.getName().getString() : "").text(named ? "greatsage.voice.meishou" : "greatsage.voice.gaitou_nashi");
                });
                break;
            }
            case "coordinates": {
                BlockPos pos = player.blockPosition();
                out.say("voice.koku").say("reply.zahyou");
                out.clips.addAll(NumberSpeech.of(pos.getX()));
                out.clips.addAll(NumberSpeech.of(pos.getY()));
                out.clips.addAll(NumberSpeech.of(pos.getZ()));
                out.say("num.desu").arg(pos.getX() + " " + pos.getY() + " " + pos.getZ()).text("greatsage.voice.zahyou");
                break;
            }
            case "altitude": {
                out.say("voice.koku").value("reply.koudo", player.blockPosition().getY()).text("greatsage.voice.koudo");
                break;
            }
            case "dimension": {
                out.say("voice.koku").say("reply.jigen").say("num.desu").arg(player.level().dimension().location().getPath()).text("greatsage.voice.jigen");
                break;
            }
            case "time": {
                long day = player.level().getDayTime() % 24000L;
                out.say("voice.koku").say(day < 12000L ? "reply.hiru" : "reply.yoru").value("reply.jikoku", (double)day / 1000.0).text("greatsage.voice.jikoku");
                break;
            }
            case "until_dawn": {
                long day = player.level().getDayTime() % 24000L;
                long left = day < 23000L ? 23000L - day : 24000L - day + 23000L;
                out.say("voice.koku").value("reply.yoake_made", (double)left / 20.0).text("greatsage.voice.yoake_made");
                break;
            }
            case "until_night": {
                long day = player.level().getDayTime() % 24000L;
                long left = day < 13000L ? 13000L - day : 24000L - day + 13000L;
                out.say("voice.koku").value("reply.nichibotsu_made", (double)left / 20.0).text("greatsage.voice.nichibotsu_made");
                break;
            }
            case "light_level": {
                out.say("voice.koku").value("reply.meido", player.level().getMaxLocalRawBrightness(player.blockPosition())).text("greatsage.voice.meido");
                break;
            }
            case "under_sky": {
                boolean sky = player.level().canSeeSky(player.blockPosition());
                out.say("voice.kai").say(sky ? "reply.chijou" : "reply.chika").say("num.desu").text(sky ? "greatsage.voice.chijou" : "greatsage.voice.chika");
                break;
            }
            case "facing": {
                Answer answer = out.say("voice.koku").say("reply.houi");
                answer.say(switch (player.getDirection()) {
                    case Direction.NORTH -> "reply.hokubu";
                    case Direction.SOUTH -> "reply.nanbu";
                    case Direction.EAST -> "reply.toubu";
                    default -> "reply.seibu";
                }).say("num.desu").text("greatsage.voice.houi");
                break;
            }
            case "weather": {
                boolean rain = player.level().isRaining();
                out.say("voice.koku").say("reply.tenkou").say("num.desu").arg(rain ? "rain" : "clear").text("greatsage.voice.tenkou");
                break;
            }
            case "world_age": {
                out.say("voice.koku").value("reply.keika_jikan", (double)player.level().getGameTime() / 24000.0).text("greatsage.voice.keika_jikan");
                break;
            }
            case "magicule_density": {
                out.say("voice.koku").value("reply.maso_noudo", VoiceService.chunkMagicule(player)).text("greatsage.voice.maso_noudo");
                break;
            }
            case "toggle_skill": 
            case "skill_on": 
            case "skill_off": {
                boolean wanted;
                ManasSkillInstance held = VoiceService.find(player, argument);
                if (held == null) {
                    out.say("voice.hi").say("voice.mishutoku").arg(argument).text("greatsage.voice.mishutoku");
                    break;
                }
                switch (command) {
                    case "skill_on": {
                        boolean bl = true;
                        break;
                    }
                    case "skill_off": {
                        boolean bl = false;
                        break;
                    }
                    default: {
                        boolean bl = wanted = !held.isToggled();
                    }
                }
                if (!VoiceService.setToggled(player, held, wanted)) {
                    out.say("voice.hi").say("reply.jikkou_fuka").arg(VoiceService.name(held)).text("greatsage.voice.jikkou_fuka");
                    break;
                }
                out.say("voice.ryo").say("reply.jikkou_kanryou").arg(VoiceService.name(held)).text(wanted ? "greatsage.voice.toggle_on" : "greatsage.voice.toggle_off");
                break;
            }
            case "skill_use": {
                ManasSkillInstance held = VoiceService.find(player, argument);
                if (held == null) {
                    out.say("voice.hi").say("voice.mishutoku").arg(argument).text("greatsage.voice.mishutoku");
                    break;
                }
                if (!VoiceService.trigger(player, held)) {
                    out.say("voice.hi").say("reply.jikkou_fuka").arg(VoiceService.name(held)).text("greatsage.voice.jikkou_fuka");
                    break;
                }
                out.say("voice.ryo").say("reply.jikkou").arg(VoiceService.name(held)).text("greatsage.voice.jikkou");
                break;
            }
            case "skill_state": {
                ManasSkillInstance held = VoiceService.find(player, argument);
                if (held == null) {
                    out.say("voice.hi").say("voice.mishutoku").arg(argument).text("greatsage.voice.mishutoku");
                    break;
                }
                out.arg(VoiceService.name(held));
                VoiceService.yesNo(out, held.isToggled());
                break;
            }
            case "battle_mode": 
            case "battle_mode_off": {
                boolean on = "battle_mode".equals(command);
                int changed = 0;
                changed = BattleMode.set(player, on, argument);
                out.say("voice.ryo").value(on ? "reply.sentou_kaishi" : "reply.sentou_kaijo", changed).text(on ? "greatsage.voice.battle_on" : "greatsage.voice.battle_off");
                break;
            }
            case "toggle_all_off": {
                int changed = 0;
                for (ManasSkillInstance held : VoiceService.ownedSkills(player)) {
                    if (!held.isToggled() || !VoiceService.setToggled(player, held, false)) continue;
                    ++changed;
                }
                out.say("voice.ryo").value("reply.sentou_kaijo", changed).text("greatsage.voice.battle_off");
                break;
            }
            case "list_skills": 
            case "list_unique": 
            case "list_ultimate": 
            case "list_resistance": 
            case "list_intrinsic": 
            case "list_extra": 
            case "list_common": {
                String tier = command.equals("list_skills") ? "" : command.substring(5);
                ArrayList<ManasSkillInstance> picked = new ArrayList<ManasSkillInstance>();
                for (ManasSkillInstance held : VoiceService.ownedSkills(player)) {
                    if (!tier.isEmpty() && !tier.equals(VoiceService.tierOf(held))) continue;
                    picked.add(held);
                }
                if (picked.isEmpty()) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                out.say("voice.koku").say("reply.rekkyo");
                int spoken = 0;
                StringBuilder written = new StringBuilder();
                for (ManasSkillInstance held : picked) {
                    if (spoken >= 5) break;
                    String clip = VoiceService.clipFor(held.getSkillId());
                    if (clip != null) {
                        out.say(clip);
                    }
                    if (written.length() > 0) {
                        written.append(", ");
                    }
                    written.append(VoiceService.name(held));
                    ++spoken;
                }
                out.say("num.desu").arg(String.valueOf(picked.size())).arg(written.toString()).text("greatsage.voice.rekkyo");
                break;
            }
            case "can_evolve": 
            case "next_evolution": 
            case "evolution_gap": 
            case "evolve_first": {
                List<?> next = VoiceService.nextEvolutions(player);
                if (next.isEmpty()) {
                    out.say("voice.hi").say("reply.shinka_fuka").text("greatsage.voice.shinka_fuka");
                    break;
                }
                Object target3 = next.get(0);
                String label = VoiceService.raceName(target3);
                if ("next_evolution".equals(command)) {
                    out.say("voice.koku").say("reply.tsugi_no_shuzoku").say("num.desu").arg(label).text("greatsage.voice.tsugi_no_shuzoku");
                    break;
                }
                float progress = VoiceService.evolutionProgress(player, target3);
                if ("evolve_first".equals(command)) {
                    out.say("voice.kai").say("reply.suishou_shinka").text("greatsage.voice.suishou_shinka");
                    break;
                }
                if (progress >= 1.0f) {
                    out.say("voice.ze").say("reply.shinka_kanou").text("greatsage.voice.shinka_kanou");
                    break;
                }
                out.say("voice.koku").arg(label).percent("reply.fusoku_ryou", progress * 100.0f).text("greatsage.voice.evolution_gap");
                break;
            }
            case "evolution_needs": {
                List<?> next = VoiceService.nextEvolutions(player);
                if (next.isEmpty()) {
                    out.say("voice.hi").say("reply.shinka_fuka").text("greatsage.voice.shinka_fuka");
                    break;
                }
                out.say("voice.koku").say("reply.shinka_jouken").say("num.desu").arg(VoiceService.raceName(next.get(0))).text("greatsage.voice.shinka_jouken");
                break;
            }
            case "attack": {
                out.say("voice.koku").value("reply.kougeki", player.getAttributeValue(Attributes.ATTACK_DAMAGE)).text("greatsage.voice.kougeki");
                break;
            }
            case "speed": {
                out.say("voice.koku").value("reply.sokudo", player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 100.0).text("greatsage.voice.sokudo");
                break;
            }
            case "poisoned": 
            case "effects": {
                Collection active = player.getActiveEffects();
                if (active.isEmpty()) {
                    out.say("voice.hi").say("reply.futsuu").text("greatsage.voice.futsuu");
                    break;
                }
                StringBuilder written = new StringBuilder();
                for (MobEffectInstance effect : active) {
                    if (written.length() > 0) {
                        written.append(", ");
                    }
                    written.append(((MobEffect)effect.getEffect().value()).getDisplayName().getString());
                }
                out.say("voice.koku").say("effects".equals(command) ? "reply.joutai_ichiran" : "reply.joutai_ijou").say("num.desu").arg(written.toString()).text("greatsage.voice.joutai_ijou");
                break;
            }
            case "stomach_list": {
                List<ItemStack> held = VoiceService.storedItems(player);
                if (held.isEmpty()) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                StringBuilder written = new StringBuilder();
                int shown = 0;
                for (ItemStack stack : held) {
                    if (shown >= 6) break;
                    if (written.length() > 0) {
                        written.append(", ");
                    }
                    written.append(stack.getCount()).append("x ").append(stack.getHoverName().getString());
                    ++shown;
                }
                out.say("voice.koku").say("reply.rekkyo").say("num.desu").arg(String.valueOf(held.size())).arg(written.toString()).text("greatsage.voice.rekkyo");
                break;
            }
            case "stomach_fill": 
            case "stomach_left": {
                int used = VoiceService.storedItems(player).size();
                int total = VoiceService.storageSize(player);
                if (total <= 0) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                boolean left = "stomach_left".equals(command);
                out.say("voice.koku").value(left ? "reply.zanryou" : "reply.youryou", left ? (double)(total - used) : (double)used).arg(String.valueOf(total)).text(left ? "greatsage.voice.zanryou" : "greatsage.voice.youryou");
                break;
            }
            case "stomach_has": 
            case "item_count": {
                int count = 0;
                for (ItemStack stack : VoiceService.storedItems(player)) {
                    if (!VoiceService.matches(stack, argument)) continue;
                    count += stack.getCount();
                }
                for (ItemStack stack : player.getInventory().items) {
                    if (!VoiceService.matches(stack, argument)) continue;
                    count += stack.getCount();
                }
                out.say("voice.koku").arg(argument).value("reply.suuryou", count).text("greatsage.voice.suuryou");
                break;
            }
            case "stomach_take": {
                String from = VoiceService.takeFromStorage(player, argument);
                if (from == null) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                out.say("voice.ryo").say("reply.toridashi_kanryou").arg(argument).arg(from).text("greatsage.voice.toridashi_kanryou");
                break;
            }
            case "stomach_store": {
                String receptacle;
                ItemStack inHand = player.getMainHandItem();
                String label = inHand.getHoverName().getString();
                String string = receptacle = inHand.isEmpty() ? null : VoiceService.storeItem(player, inHand);
                if (receptacle == null) {
                    out.say("voice.hi").say("reply.jikkou_fuka").arg(label).text("greatsage.voice.jikkou_fuka");
                    break;
                }
                out.say("voice.ryo").say("reply.shuunou_kanryou").arg(label).arg(receptacle).text("greatsage.voice.shuunou_kanryou");
                break;
            }
            case "free_slots": {
                int free = 0;
                for (ItemStack stack : player.getInventory().items) {
                    if (!stack.isEmpty()) continue;
                    ++free;
                }
                out.say("voice.koku").value("reply.kuuseki", free).text("greatsage.voice.kuuseki");
                break;
            }
            case "have_food": {
                ItemStack best = null;
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) continue;
                    best = stack;
                    break;
                }
                out.arg(best == null ? "" : best.getHoverName().getString());
                VoiceService.yesNo(out, best != null);
                break;
            }
            case "best_weapon": 
            case "best_pickaxe": 
            case "best_item": 
            case "better_tool": {
                ItemStack best = VoiceService.bestTool(player, "better_tool".equals(command) ? "best_pickaxe" : command);
                if (best == null) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                boolean advising = "better_tool".equals(command);
                out.say(advising ? "voice.kai" : "voice.koku").say(advising ? "reply.kouhosha_ari" : "reply.saiteki").say("num.desu").arg(best.getHoverName().getString()).text(advising ? "greatsage.voice.kouhosha_ari" : "greatsage.voice.saiteki");
                break;
            }
            case "identify_item": 
            case "item_value": {
                ItemStack inHand = player.getMainHandItem();
                if (inHand.isEmpty()) {
                    out.say("voice.hi").say("reply.taishou_nashi").text("greatsage.voice.taishou_nashi");
                    break;
                }
                if ("item_value".equals(command)) {
                    boolean precious = inHand.getRarity().ordinal() > 0;
                    out.say(precious ? "voice.ze" : "voice.hi").say(precious ? "reply.kachi_ari" : "reply.kachi_nashi").arg(inHand.getHoverName().getString()).text(precious ? "greatsage.voice.kachi_ari" : "greatsage.voice.kachi_nashi");
                    break;
                }
                out.say("voice.koku").say("reply.setsumei").say("num.desu").arg(inHand.getHoverName().getString()).arg(String.valueOf(inHand.getCount())).text("greatsage.voice.setsumei_item");
                break;
            }
            case "can_craft": 
            case "craft_needs": {
                List<String> missing = VoiceService.missingIngredients(player, argument);
                if (missing == null) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                if (missing.isEmpty()) {
                    out.say("voice.ze").say("reply.sakusei_kanou").arg(argument).text("greatsage.voice.sakusei_kanou");
                    break;
                }
                out.say("voice.hi").say("reply.fusoku_zairyou").say("num.desu").arg(String.join((CharSequence)", ", missing)).text("greatsage.voice.fusoku_zairyou");
                break;
            }
            case "find_ore": 
            case "find_diamond": 
            case "find_iron": 
            case "find_gold": 
            case "find_magisteel": 
            case "find_lava": 
            case "find_water": 
            case "find_cave": {
                BlockPos found = VoiceService.search(player, command);
                if (found == null) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                String frame = switch (command) {
                    case "find_lava" -> "reply.netsugen_ari";
                    case "find_water" -> "reply.suigen_ari";
                    case "find_cave" -> "reply.kuudou_ari";
                    default -> "reply.koumyaku_ari";
                };
                out.say("voice.koku").say(frame).value("reply.kyori", Math.sqrt(player.blockPosition().distSqr((Vec3i)found))).arg(VoiceService.bearing(player, found)).text("greatsage.voice.found_at");
                break;
            }
            case "protect": {
                int raised = 0;
                for (ManasSkillInstance held : VoiceService.ownedSkills(player)) {
                    if (!VoiceService.isBarrier(player, held) || !VoiceService.setToggled(player, held, true)) continue;
                    ++raised;
                }
                boolean shielded = VoiceService.equipShield(player);
                if (raised == 0 && !shielded) {
                    out.say("voice.hi").say("reply.jikkou_fuka").arg("").text("greatsage.voice.protect_none");
                    break;
                }
                out.say("voice.ryo").value("reply.shugo_tenkai", raised).arg(shielded ? "1" : "0").text("greatsage.voice.protect");
                break;
            }
            case "heal": {
                int slot = VoiceService.findRemedy(player);
                if (slot < 0) {
                    out.say("voice.hi").say("reply.chiyu_shudan_nashi").text("greatsage.voice.heal_none");
                    break;
                }
                String label = VoiceService.consume(player, slot);
                out.say("voice.ryo").say("reply.chiyu").arg(label).text("greatsage.voice.heal");
                break;
            }
            case "find_village": 
            case "find_labyrinth": {
                out.say("voice.hi").say("reply.kanshou_kyohi").text("greatsage.voice.refuse_cheat");
                break;
            }
            case "is_demon_lord": 
            case "is_hero": {
                String path;
                ResourceLocation race = VoiceService.raceId((LivingEntity)player);
                String string = path = race == null ? "" : race.getPath();
                VoiceService.yesNo(out, "is_hero".equals(command) ? path.contains("hero") : path.contains("demon_lord") || path.contains("demonlord"));
                break;
            }
            case "last_learned": {
                ManasSkillInstance recent = VoiceService.rarest(player);
                if (recent == null) break;
                out.say("voice.kai").named("reply.chokuzen", VoiceService.clipFor(recent.getSkillId()), VoiceService.name(recent)).text("greatsage.voice.chokuzen");
                break;
            }
            case "what_skill": 
            case "skill_effect": {
                Skill target4 = VoiceService.lookup(argument);
                if (target4 == null) break;
                out.say("voice.kai").say("reply.setsumei").say("num.desu").arg(target4.getName().getString()).arg(target4.getType() == null ? "?" : target4.getType().getSerializedName()).text("greatsage.voice.setsumei_skill");
                break;
            }
            case "can_learn": {
                Skill target5 = VoiceService.lookup(argument);
                if (target5 == null) break;
                boolean owned = VoiceService.find(player, argument) != null;
                out.say(owned ? "voice.hi" : "voice.kai").say(owned ? "reply.shutoku_fuka" : "reply.shutoku_kanou").arg(target5.getName().getString()).text(owned ? "greatsage.voice.shutoku_fuka" : "greatsage.voice.shutoku_kanou");
                break;
            }
            case "unused_skills": 
            case "race_skills": {
                ArrayList<ManasSkillInstance> idle = new ArrayList<ManasSkillInstance>();
                for (ManasSkillInstance held : VoiceService.ownedSkills(player)) {
                    if (!(held.getMastery() <= 0.0)) continue;
                    idle.add(held);
                }
                if (idle.isEmpty()) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                out.say("voice.kai").value("reply.rekkyo", idle.size()).arg(VoiceService.name((ManasSkillInstance)idle.get(0))).text("greatsage.voice.idle_skills");
                break;
            }
            case "analysis_progress": 
            case "analysis_target": {
                ResourceLocation running = AnalysisManager.analysedSkill(player);
                if (running == null) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                if ("analysis_target".equals(command)) {
                    out.say("voice.kai").say("reply.kaiseki_taishou").say("num.desu").arg(running.getPath()).text("greatsage.voice.kaiseki_taishou");
                    break;
                }
                out.say("voice.kai").arg(running.getPath()).percent("reply.shinchoku", AnalysisManager.progress(player) * 100.0).text("greatsage.voice.shinchoku");
                break;
            }
            case "analyse_weakest": 
            case "analysis_advice": 
            case "skill_advice": 
            case "advice": 
            case "food_advice": {
                ManasSkillInstance pick = null;
                for (ManasSkillInstance held : VoiceService.ownedSkills(player)) {
                    if (held.isMastered((LivingEntity)player) || "resistance".equals(VoiceService.tierOf(held))) continue;
                    pick = held;
                    break;
                }
                if (pick == null) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                out.say("voice.kai").named("reply.suishou", VoiceService.clipFor(pick.getSkillId()), VoiceService.name(pick)).text("greatsage.voice.suishou");
                break;
            }
            case "target_skills": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    ArrayList<String> names = new ArrayList<String>();
                    for (ManasSkillInstance held : SkillAPI.getSkillsFrom((LivingEntity)target).getLearnedSkills()) {
                        ManasSkill patt0$temp = held.getSkill();
                        if (!(patt0$temp instanceof Skill)) continue;
                        Skill skill = (Skill)patt0$temp;
                        if (!held.isMastered(target)) continue;
                        names.add(skill.getName().getString());
                    }
                    if (names.isEmpty()) {
                        out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                        return;
                    }
                    out.say("voice.kai").value("reply.rekkyo", names.size()).arg(String.join((CharSequence)", ", names.subList(0, Math.min(5, names.size())))).text("greatsage.voice.rekkyo");
                });
                break;
            }
            case "target_awakened": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    ResourceLocation race = VoiceService.raceId(target);
                    VoiceService.yesNo(out, race != null && race.getPath().contains("demon_lord"));
                });
                break;
            }
            case "one_shot": {
                VoiceService.withTarget(player, targetId, out, target -> VoiceService.yesNo(out, EnergyHelper.getMaxEP((LivingEntity)target) > EnergyHelper.getMaxEP((LivingEntity)player) * 3.0));
                break;
            }
            case "winning": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    boolean ahead = player.getHealth() / player.getMaxHealth() >= target.getHealth() / target.getMaxHealth();
                    out.say(ahead ? "voice.ze" : "voice.hi").say(ahead ? "reply.yuui" : "reply.retsui").text(ahead ? "greatsage.voice.yuui" : "greatsage.voice.retsui");
                });
                break;
            }
            case "survival_time": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    double theirs = Math.max(1.0, EnergyHelper.getMaxEP((LivingEntity)target));
                    out.say("voice.koku").value("reply.seizon_jikan", (double)player.getHealth() * 20.0 / theirs).text("greatsage.voice.seizon_jikan");
                });
                break;
            }
            case "incoming_damage": {
                VoiceService.withTarget(player, targetId, out, target -> out.say("voice.koku").value("reply.souteki_higai", target.getAttributeValue(Attributes.ATTACK_DAMAGE)).text("greatsage.voice.souteki_higai"));
                break;
            }
            case "target_weakness": {
                VoiceService.withTarget(player, targetId, out, target -> out.say("voice.kai").say("reply.jakuten").say("num.desu").arg(target.fireImmune() ? "not fire" : "fire").text("greatsage.voice.jakuten"));
                break;
            }
            case "can_predate": 
            case "predate_gain": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    boolean weaker = EnergyHelper.getMaxEP((LivingEntity)target) < EnergyHelper.getMaxEP((LivingEntity)player);
                    out.say(weaker ? "voice.ze" : "voice.hi").say(weaker ? "reply.hoshoku_kanou" : "reply.hoshoku_fuka").text(weaker ? "greatsage.voice.hoshoku_kanou" : "greatsage.voice.hoshoku_fuka");
                });
                break;
            }
            case "followed": {
                LivingEntity back = VoiceService.behind(player);
                out.say(back == null ? "voice.hi" : "voice.koku").say(back == null ? "reply.kiken_nashi" : "reply.tsuiseki_ari").text(back == null ? "greatsage.voice.kiken_nashi" : "greatsage.voice.tsuiseki_ari");
                break;
            }
            case "should_run": {
                VoiceService.withTarget(player, targetId, out, target -> {
                    boolean flee = EnergyHelper.getMaxEP((LivingEntity)target) > EnergyHelper.getMaxEP((LivingEntity)player);
                    out.say(flee ? "voice.koku" : "voice.hi").say(flee ? "reply.tettai_suishou" : "reply.hitsuyou_nashi").text(flee ? "greatsage.voice.tettai_suishou" : "greatsage.voice.hitsuyou_nashi");
                });
                break;
            }
            case "biome": {
                out.say("voice.kai").say("reply.baiomu").say("num.desu").arg(player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location().getPath()).orElse("?")).text("greatsage.voice.baiomu");
                break;
            }
            case "will_storm": {
                VoiceService.yesNo(out, player.level().isThundering());
                break;
            }
            case "spawn_distance": {
                BlockPos spawn = player.getRespawnPosition() != null ? player.getRespawnPosition() : player.level().getSharedSpawnPos();
                out.say("voice.koku").value("reply.kyori", Math.sqrt(player.blockPosition().distSqr((Vec3i)spawn))).text("greatsage.voice.kyori");
                break;
            }
            case "drop_below": 
            case "fall_safe": {
                int drop;
                BlockPos at = player.blockPosition().below();
                for (drop = 0; drop < 32 && player.level().getBlockState(at).isAir(); ++drop) {
                    at = at.below();
                }
                VoiceService.yesNo(out, drop < 4);
                break;
            }
            case "safe_to_sleep": 
            case "good_build_spot": {
                boolean safe = VoiceService.strongestNearby(player) == null && player.level().getMaxLocalRawBrightness(player.blockPosition()) > 7;
                out.say(safe ? "voice.ze" : "voice.hi").say(safe ? "reply.futsuu" : "reply.futekisetsu").text(safe ? "greatsage.voice.futsuu" : "greatsage.voice.futekisetsu");
                break;
            }
            case "equip_best": {
                int changed = VoiceService.equipBestArmour(player);
                if (changed == 0) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                out.say("voice.ryo").value("reply.souchaku_kanryou", changed).text("greatsage.voice.souchaku_armour");
                break;
            }
            case "sort_inventory": 
            case "sort_by_type": {
                VoiceService.tidy(player, out, Order.TYPE);
                break;
            }
            case "sort_by_name": {
                VoiceService.tidy(player, out, Order.NAME);
                break;
            }
            case "sort_by_count": {
                VoiceService.tidy(player, out, Order.COUNT);
                break;
            }
            case "drop_item": {
                ItemStack inHand = player.getMainHandItem();
                if (inHand.isEmpty()) {
                    out.say("voice.hi").say("reply.taishou_nashi").text("greatsage.voice.taishou_nashi");
                    break;
                }
                String label = inHand.getHoverName().getString();
                player.drop(inHand.copy(), false);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                out.say("voice.ryo").say("reply.haiki_kanryou").arg(label).text("greatsage.voice.haiki_kanryou");
                break;
            }
            case "repair": {
                out.say("voice.hi").say("reply.jikkou_fuka").arg("").text("greatsage.voice.repair_none");
                break;
            }
            case "lost_items": {
                out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                break;
            }
            case "eat": {
                int slot = -1;
                for (int i = 0; i < player.getInventory().items.size(); ++i) {
                    ItemStack stack = (ItemStack)player.getInventory().items.get(i);
                    if (stack.isEmpty() || !stack.has(DataComponents.FOOD) || VoiceService.harmful(stack)) continue;
                    slot = i;
                    break;
                }
                if (slot < 0) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                String label = VoiceService.consume(player, slot);
                out.say("voice.ryo").say("reply.jikkou").arg(label).text("greatsage.voice.heal");
                break;
            }
            case "craft": {
                List<String> missing = VoiceService.missingIngredients(player, argument);
                if (missing == null) {
                    out.say("voice.hi").say("reply.gaitou_nashi").text("greatsage.voice.gaitou_nashi");
                    break;
                }
                if (!missing.isEmpty()) {
                    out.say("voice.hi").say("reply.zairyou_fusoku").say("num.desu").arg(String.join((CharSequence)", ", missing)).text("greatsage.voice.fusoku_zairyou");
                    break;
                }
                out.say("voice.ze").say("reply.sakusei_kanou").arg(argument).text("greatsage.voice.sakusei_kanou");
                break;
            }
            case "what_killed_me": {
                String cause;
                String string = cause = player.getLastDamageSource() == null ? "" : player.getLastDamageSource().getMsgId();
                if (cause.isEmpty()) {
                    out.say("voice.hi").say("reply.jouhou_nashi").text("greatsage.voice.jouhou_nashi");
                    break;
                }
                out.say("voice.kai").say("reply.shiin").say("num.desu").arg(cause).text("greatsage.voice.shiin");
                break;
            }
        }
    }

    private static boolean setToggled(ServerPlayer player, ManasSkillInstance instance, boolean on) {
        if (!instance.canBeToggled((LivingEntity)player) || instance.isToggled() == on) {
            return false;
        }
        instance.setToggled(on);
        if (on) {
            instance.onToggleOn((LivingEntity)player);
        } else {
            instance.onToggleOff((LivingEntity)player);
        }
        SkillAPI.getSkillsFrom((LivingEntity)player).updateSkill(instance, true);
        return true;
    }

    private static boolean trigger(ServerPlayer player, ManasSkillInstance instance) {
        try {
            if (instance.onCoolDown(0) || !instance.canActivateSkill((LivingEntity)player, 0)) {
                return false;
            }
            instance.onPressed((LivingEntity)player, 0, 0);
            instance.onRelease((LivingEntity)player, 0, 0, 0);
            SkillAPI.getSkillsFrom((LivingEntity)player).updateSkill(instance, true);
            return true;
        }
        catch (Throwable failure) {
            return false;
        }
    }

    private static String tierOf(ManasSkillInstance instance) {
        Skill skill;
        ManasSkill manasSkill = instance.getSkill();
        return manasSkill instanceof Skill && (skill = (Skill)manasSkill).getType() != null ? skill.getType().getSerializedName().toLowerCase(Locale.ROOT) : "";
    }

    private static List<?> nextEvolutions(ServerPlayer player) {
        try {
            ManasRaceInstance instance = RaceAPI.getRaceFrom((LivingEntity)player).getRace().orElse(null);
            if (instance == null || instance.getRace() == null) {
                return List.of();
            }
            return instance.getRace().getNextEvolutions(instance, (LivingEntity)player);
        }
        catch (RuntimeException failure) {
            return List.of();
        }
    }

    private static float evolutionProgress(ServerPlayer player, Object target) {
        try {
            ManasRaceInstance instance = RaceAPI.getRaceFrom((LivingEntity)player).getRace().orElse(null);
            if (instance == null || instance.getRace() == null) {
                return 0.0f;
            }
            return instance.getRace().getEvolutionProgress(instance, (LivingEntity)player, (ManasRace)target);
        }
        catch (RuntimeException failure) {
            return 0.0f;
        }
    }

    private static String raceName(Object race) {
        String string;
        if (race instanceof ManasRace) {
            ManasRace manas = (ManasRace)race;
            string = manas.getName().getString();
        } else {
            string = String.valueOf(race);
        }
        return string;
    }

    private static List<SpatialStorageContainer> storages(ServerPlayer player) {
        ArrayList<SpatialStorageContainer> out = new ArrayList<SpatialStorageContainer>();
        for (ManasSkillInstance instance : VoiceService.storageSkills(player)) {
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof ISpatialStorage)) continue;
            ISpatialStorage holder = (ISpatialStorage)manasSkill;
            try {
                SpatialStorageContainer container = holder.getSpatialStorage(instance, (HolderLookup.Provider)player.registryAccess());
                if (container == null) continue;
                out.add(container);
            }
            catch (RuntimeException runtimeException) {}
        }
        return out;
    }

    private static List<ManasSkillInstance> storageSkills(ServerPlayer player) {
        ArrayList<ManasSkillInstance> out = new ArrayList<ManasSkillInstance>();
        ArrayList<ManasSkillInstance> late = new ArrayList<ManasSkillInstance>();
        for (ManasSkillInstance instance : VoiceService.ownedSkills(player)) {
            String label;
            if (!(instance.getSkill() instanceof ISpatialStorage) || (label = VoiceService.plain(VoiceService.name(instance))).contains("great sage") || label.contains("raphael") || label.contains("ciel")) continue;
            if (label.contains("predator") || label.contains("gluttony")) {
                out.add(instance);
                continue;
            }
            late.add(instance);
        }
        out.addAll(late);
        return out;
    }

    private static List<ItemStack> storedItems(ServerPlayer player) {
        ArrayList<ItemStack> out = new ArrayList<ItemStack>();
        for (SpatialStorageContainer container : VoiceService.storages(player)) {
            for (int slot = 0; slot < container.getContainerSize(); ++slot) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) continue;
                out.add(stack);
            }
        }
        return out;
    }

    private static int storageSize(ServerPlayer player) {
        int total = 0;
        for (SpatialStorageContainer container : VoiceService.storages(player)) {
            total += container.getContainerSize();
        }
        return total;
    }

    @Nullable
    private static String takeFromStorage(ServerPlayer player, String wanted) {
        for (ManasSkillInstance instance : VoiceService.storageSkills(player)) {
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof ISpatialStorage)) continue;
            ISpatialStorage holder = (ISpatialStorage)manasSkill;
            try {
                SpatialStorageContainer container = holder.getSpatialStorage(instance, (HolderLookup.Provider)player.registryAccess());
                if (container == null) continue;
                for (int slot = 0; slot < container.getContainerSize(); ++slot) {
                    ItemStack stack = container.getItem(slot);
                    if (stack.isEmpty() || !VoiceService.matches(stack, wanted)) continue;
                    ItemStack taken = container.removeItemNoUpdate(slot);
                    holder.saveContainer(instance, (LivingEntity)player, container);
                    if (!player.getInventory().add(taken)) {
                        player.drop(taken, false);
                    }
                    SkillAPI.getSkillsFrom((LivingEntity)player).updateSkill(instance, true);
                    return VoiceService.name(instance);
                }
            }
            catch (RuntimeException runtimeException) {
            }
        }
        return null;
    }

    @Nullable
    private static String storeItem(ServerPlayer player, ItemStack stack) {
        for (ManasSkillInstance instance : VoiceService.storageSkills(player)) {
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof ISpatialStorage)) continue;
            ISpatialStorage holder = (ISpatialStorage)manasSkill;
            try {
                if (!holder.addItemToSpatialStorage(instance, (LivingEntity)player, stack.copy())) continue;
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                SkillAPI.getSkillsFrom((LivingEntity)player).updateSkill(instance, true);
                return VoiceService.name(instance);
            }
            catch (RuntimeException runtimeException) {
            }
        }
        return null;
    }

    private static boolean matches(ItemStack stack, String wanted) {
        if (stack.isEmpty() || wanted.isBlank()) {
            return false;
        }
        String label = VoiceService.plain(stack.getHoverName().getString());
        String needle = VoiceService.plain(wanted);
        return !needle.isEmpty() && (label.equals(needle) || label.contains(needle) || needle.contains(label));
    }

    @Nullable
    private static ItemStack bestTool(ServerPlayer player, String command) {
        ItemStack best = null;
        double bestScore = 0.0;
        for (ItemStack stack : player.getInventory().items) {
            double score;
            if (stack.isEmpty() || !((score = "best_weapon".equals(command) ? (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem ? (double)stack.getMaxDamage() + 1.0 : 0.0) : ("best_pickaxe".equals(command) ? (stack.getItem() instanceof PickaxeItem ? (double)stack.getMaxDamage() + 1.0 : 0.0) : (double)stack.getRarity().ordinal() + 1.0)) > bestScore)) continue;
            bestScore = score;
            best = stack;
        }
        return best;
    }

    @Nullable
    private static List<String> missingIngredients(ServerPlayer player, String wanted) {
        if (player.getServer() == null) {
            return null;
        }
        for (RecipeHolder holder : player.getServer().getRecipeManager().getRecipes()) {
            ItemStack result;
            try {
                result = holder.value().getResultItem((HolderLookup.Provider)player.registryAccess());
            }
            catch (RuntimeException ignored) {
                continue;
            }
            if (!VoiceService.matches(result, wanted)) continue;
            ArrayList<String> missing = new ArrayList<String>();
            for (Ingredient ingredient : holder.value().getIngredients()) {
                if (ingredient.isEmpty()) continue;
                boolean found = false;
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.isEmpty() || !ingredient.test(stack)) continue;
                    found = true;
                    break;
                }
                if (found) continue;
                ItemStack[] options = ingredient.getItems();
                missing.add(options.length == 0 ? "?" : options[0].getHoverName().getString());
            }
            return missing;
        }
        return null;
    }

    @Nullable
    private static BlockPos search(ServerPlayer player, String command) {
        BlockPos at = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)at.offset(-16, -16, -16), (BlockPos)at.offset(16, 16, 16))) {
            double distance;
            if (!VoiceService.wanted(player, pos, command) || !((distance = at.distSqr((Vec3i)pos)) < bestDistance)) continue;
            bestDistance = distance;
            best = pos.immutable();
        }
        return best;
    }

    private static boolean wanted(ServerPlayer player, BlockPos pos, String command) {
        BlockState state = player.level().getBlockState(pos);
        return switch (command) {
            case "find_diamond" -> state.is(BlockTags.DIAMOND_ORES);
            case "find_iron" -> state.is(BlockTags.IRON_ORES);
            case "find_gold" -> state.is(BlockTags.GOLD_ORES);
            case "find_lava" -> state.getFluidState().is(FluidTags.LAVA);
            case "find_water" -> state.getFluidState().is(FluidTags.WATER);
            case "find_cave" -> {
                if (state.isAir() && !player.level().canSeeSky(pos) && pos.getY() < player.blockPosition().getY() - 2) {
                    yield true;
                }
                yield false;
            }
            case "find_magisteel" -> state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT).contains("magisteel");
            default -> state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES) || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.IRON_ORES);
        };
    }

    private static String bearing(ServerPlayer player, BlockPos target) {
        String horizontal;
        BlockPos at = player.blockPosition();
        int dx = target.getX() - at.getX();
        int dz = target.getZ() - at.getZ();
        int dy = target.getY() - at.getY();
        String string = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (horizontal = dz > 0 ? "south" : "north");
        if (Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz))) {
            return dy > 0 ? "above" : "below";
        }
        return horizontal;
    }

    private static boolean isBarrier(ServerPlayer player, ManasSkillInstance instance) {
        if (instance.getSkill() == null || !instance.canBeToggled((LivingEntity)player)) {
            return false;
        }
        String label = instance.getSkill().getName().getString().toLowerCase(Locale.ROOT);
        return label.contains("barrier") || label.contains("shield") || label.contains("armour") || label.contains("armor") || label.contains("guard") || label.contains("protection") || label.contains("defen");
    }

    private static boolean equipShield(ServerPlayer player) {
        ItemStack current = player.getOffhandItem();
        if (current.getItem() instanceof ShieldItem) {
            return true;
        }
        for (int slot = 0; slot < player.getInventory().items.size(); ++slot) {
            ItemStack stack = (ItemStack)player.getInventory().items.get(slot);
            if (!(stack.getItem() instanceof ShieldItem)) continue;
            player.getInventory().items.set(slot, (Object)ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
            if (!current.isEmpty() && !player.getInventory().add(current)) {
                player.drop(current, false);
            }
            return true;
        }
        return false;
    }

    private static int findRemedy(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().items.size(); ++i) {
            ItemStack stack = (ItemStack)player.getInventory().items.get(i);
            if (stack.isEmpty() || VoiceService.harmful(stack)) continue;
            if (stack.getItem() instanceof PotionItem && VoiceService.beneficial(stack)) {
                return i;
            }
            String label = VoiceService.plain(stack.getHoverName().getString());
            if (!label.contains("heal") && !label.contains("potion") && !label.contains("elixir") && !label.contains("recovery") && !label.contains("cure") && !label.contains("remedy") && !label.contains("medicine")) continue;
            return i;
        }
        return -1;
    }

    private static boolean harmful(ItemStack stack) {
        PotionContents contents = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (((MobEffect)effect.getEffect().value()).isBeneficial()) continue;
            return true;
        }
        return false;
    }

    private static boolean beneficial(ItemStack stack) {
        PotionContents contents = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (!((MobEffect)effect.getEffect().value()).isBeneficial()) continue;
            return true;
        }
        return false;
    }

    private static String consume(ServerPlayer player, int slot) {
        ItemStack stack = (ItemStack)player.getInventory().items.get(slot);
        String label = stack.getHoverName().getString();
        ItemStack rest = stack.finishUsingItem(player.level(), (LivingEntity)player);
        player.getInventory().items.set(slot, (Object)(rest == null ? ItemStack.EMPTY : rest));
        return label;
    }

    private static void tidy(ServerPlayer player, Answer out, Order order) {
        Inventory inventory = player.getInventory();
        ArrayList<ItemStack> loose = new ArrayList<ItemStack>();
        for (int slot = 9; slot < inventory.items.size(); ++slot) {
            ItemStack stack = (ItemStack)inventory.items.get(slot);
            if (stack.isEmpty()) continue;
            loose.add(stack.copy());
        }
        if (loose.isEmpty()) {
            out.say("voice.hi").say("reply.taishou_nashi").text("greatsage.voice.taishou_nashi");
            return;
        }
        List<ItemStack> piles = VoiceService.stacked(loose);
        piles.sort(VoiceService.ordering(order));
        for (int slot = 9; slot < inventory.items.size(); ++slot) {
            int rank = slot - 9;
            inventory.items.set(slot, (Object)(rank < piles.size() ? piles.get(rank) : ItemStack.EMPTY));
        }
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        out.say("voice.ryo").value("reply.seiri_kanryou", piles.size()).text("greatsage.voice.seiri_kanryou");
    }

    private static List<ItemStack> stacked(List<ItemStack> loose) {
        ArrayList<ItemStack> piles = new ArrayList<ItemStack>();
        for (ItemStack stack : loose) {
            for (ItemStack pile : piles) {
                if (pile.getCount() >= pile.getMaxStackSize() || !ItemStack.isSameItemSameComponents((ItemStack)pile, (ItemStack)stack)) continue;
                int moved = Math.min(pile.getMaxStackSize() - pile.getCount(), stack.getCount());
                pile.grow(moved);
                stack.shrink(moved);
                if (!stack.isEmpty()) continue;
                break;
            }
            if (stack.isEmpty()) continue;
            piles.add(stack);
        }
        return piles;
    }

    private static Comparator<ItemStack> ordering(Order order) {
        Comparator<ItemStack> byName = Comparator.comparing(stack -> VoiceService.plain(stack.getHoverName().getString()));
        return switch (order.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> byName;
            case 1 -> Comparator.comparingInt(ItemStack::getCount).reversed().thenComparing(byName);
            case 2 -> Comparator.comparingInt(VoiceService::family).thenComparing(VoiceService::registryKey).thenComparing(byName);
        };
    }

    private static int family(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem) {
            return 0;
        }
        if (item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem) {
            return 1;
        }
        if (item instanceof DiggerItem) {
            return 2;
        }
        if (stack.has(DataComponents.FOOD)) {
            return 3;
        }
        if (item instanceof BlockItem) {
            return 4;
        }
        return 5;
    }

    private static String registryKey(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey((Object)stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static int equipBestArmour(ServerPlayer player) {
        int changed = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack worn = player.getItemBySlot(slot);
            double best = VoiceService.armourValue(worn, slot);
            int bestIndex = -1;
            for (int i = 0; i < player.getInventory().items.size(); ++i) {
                double value = VoiceService.armourValue((ItemStack)player.getInventory().items.get(i), slot);
                if (!(value > best)) continue;
                best = value;
                bestIndex = i;
            }
            if (bestIndex < 0) continue;
            ItemStack better = (ItemStack)player.getInventory().items.get(bestIndex);
            player.getInventory().items.set(bestIndex, (Object)ItemStack.EMPTY);
            player.setItemSlot(slot, better);
            if (!worn.isEmpty() && !player.getInventory().add(worn)) {
                player.drop(worn, false);
            }
            ++changed;
        }
        return changed;
    }

    private static double armourValue(ItemStack stack, EquipmentSlot slot) {
        ArmorItem armour;
        Item item;
        if (stack.isEmpty() || !((item = stack.getItem()) instanceof ArmorItem) || (armour = (ArmorItem)item).getEquipmentSlot() != slot) {
            return 0.0;
        }
        return (double)armour.getDefense() * 100.0 + (double)armour.getToughness();
    }

    private static void withTarget(ServerPlayer player, int targetId, Answer out, TargetAction action) {
        LivingEntity target = VoiceService.target(player, targetId);
        if (target == null) {
            out.say("voice.hi").say("reply.taishou_nashi").text("greatsage.voice.taishou_nashi");
            return;
        }
        action.run(target);
    }

    private static void yesNo(Answer out, boolean yes) {
        out.say(yes ? "voice.ze" : "voice.hi").say(yes ? "reply.ze" : "reply.hi").text(yes ? "greatsage.voice.ze" : "greatsage.voice.hi");
    }

    @Nullable
    private static LivingEntity target(ServerPlayer player, int targetId) {
        Entity entity;
        if (targetId >= 0 && (entity = player.level().getEntity(targetId)) instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            return living;
        }
        return null;
    }

    private static List<LivingEntity> nearby(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(24.0);
        ArrayList<LivingEntity> out = new ArrayList<LivingEntity>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity == player || !entity.isAlive()) continue;
            out.add(entity);
        }
        return out;
    }

    @Nullable
    private static LivingEntity strongestNearby(ServerPlayer player) {
        return VoiceService.nearby(player).stream().max(Comparator.comparingDouble(EnergyHelper::getMaxEP)).orElse(null);
    }

    @Nullable
    private static LivingEntity behind(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        for (LivingEntity entity : VoiceService.nearby(player)) {
            Vec3 toward;
            if (player.distanceTo((Entity)entity) > 8.0f || !(look.dot(toward = entity.position().subtract(player.position()).normalize()) < -0.2)) continue;
            return entity;
        }
        return null;
    }

    private static double magicule(ServerPlayer player) {
        IExistence existence = TensuraStorages.getExistenceFrom((LivingEntity)player);
        return existence == null ? 0.0 : existence.getMagicule();
    }

    private static double maxMagicule(ServerPlayer player) {
        return Math.max(1.0, EnergyHelper.getMaxEP((LivingEntity)player));
    }

    private static double chunkMagicule(ServerPlayer player) {
        ChunkStorage chunk = TensuraStorages.getChunkFrom((LevelChunk)player.level().getChunkAt(player.blockPosition()));
        return chunk == null ? 0.0 : chunk.getMagicule();
    }

    @Nullable
    private static ResourceLocation raceId(LivingEntity entity) {
        return RaceAPI.getRaceFrom((LivingEntity)entity).getRace().map(instance -> instance.getRace().getRegistryName()).orElse(null);
    }

    private static List<ManasSkillInstance> ownedSkills(ServerPlayer player) {
        ArrayList<ManasSkillInstance> out = new ArrayList<ManasSkillInstance>();
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom((LivingEntity)player).getLearnedSkills()) {
            if (!(instance.getSkill() instanceof Skill)) continue;
            out.add(instance);
        }
        return out;
    }

    @Nullable
    private static ManasSkillInstance rarest(ServerPlayer player) {
        ManasSkillInstance best = null;
        int bestRank = -1;
        for (ManasSkillInstance instance : VoiceService.ownedSkills(player)) {
            int rank;
            Skill skill;
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof Skill) || (skill = (Skill)manasSkill).getType() == null) continue;
            if ((rank = (switch (skill.getType().getSerializedName().toLowerCase(Locale.ROOT)) {
                case "ultimate" -> 5;
                case "unique" -> 4;
                case "extra" -> 3;
                case "common" -> 2;
                case "intrinsic" -> 1;
                default -> 0;
            })) <= bestRank) continue;
            bestRank = rank;
            best = instance;
        }
        return best;
    }

    @Nullable
    private static String plain(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static ManasSkillInstance find(ServerPlayer player, String spoken) {
        String needle = VoiceService.plain(spoken);
        if (needle.isEmpty()) {
            return null;
        }
        for (ManasSkillInstance instance : VoiceService.ownedSkills(player)) {
            if (!VoiceService.plain(VoiceService.name(instance)).equals(needle)) continue;
            return instance;
        }
        for (ManasSkillInstance instance : VoiceService.ownedSkills(player)) {
            String label = VoiceService.plain(VoiceService.name(instance));
            if (!label.contains(needle) && !needle.contains(label)) continue;
            return instance;
        }
        return null;
    }

    @Nullable
    private static Skill lookup(String spoken) {
        String needle = VoiceService.plain(spoken);
        Skill loose = null;
        for (ManasSkill entry : SkillAPI.getSkillRegistry()) {
            if (!(entry instanceof Skill)) continue;
            Skill skill = (Skill)entry;
            String label = VoiceService.plain(skill.getName().getString());
            if (label.equals(needle)) {
                return skill;
            }
            if (loose != null || !label.contains(needle)) continue;
            loose = skill;
        }
        return loose;
    }

    private static String name(ManasSkillInstance instance) {
        String string;
        ManasSkill manasSkill = instance.getSkill();
        if (manasSkill instanceof Skill) {
            Skill skill = (Skill)manasSkill;
            string = skill.getName().getString();
        } else {
            string = String.valueOf(instance.getSkillId());
        }
        return string;
    }

    @Nullable
    private static String clipFor(@Nullable ResourceLocation id) {
        return id == null ? null : "skill." + id.getPath();
    }

    private static final class Answer {
        private final List<String> clips = new ArrayList<String>();
        private String subtitle = "";
        private final List<String> args = new ArrayList<String>();

        private Answer() {
        }

        Answer say(String clip) {
            this.clips.add(clip);
            return this;
        }

        Answer value(String frame, double number) {
            this.clips.add(frame);
            this.clips.addAll(NumberSpeech.of(number));
            this.clips.add("num.desu");
            this.args.add(NumberSpeech.written(number));
            return this;
        }

        Answer percent(String frame, double number) {
            this.clips.add(frame);
            this.clips.addAll(NumberSpeech.percent(number));
            this.clips.add("num.desu");
            this.args.add(Math.round(number) + "%");
            return this;
        }

        Answer named(String frame, String clip, String written) {
            this.clips.add(frame);
            if (clip != null) {
                this.clips.add(clip);
            }
            this.args.add(written);
            return this;
        }

        Answer arg(String written) {
            this.args.add(written);
            return this;
        }

        Answer text(String key) {
            this.subtitle = key;
            return this;
        }
    }

    private static interface TargetAction {
        public void run(LivingEntity var1);
    }

    private static enum Order {
        NAME,
        COUNT,
        TYPE;

    }
}

