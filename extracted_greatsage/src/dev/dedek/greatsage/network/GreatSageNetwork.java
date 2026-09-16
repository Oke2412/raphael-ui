/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
 *  net.neoforged.neoforge.network.registration.PayloadRegistrar
 */
package dev.dedek.greatsage.network;

import dev.dedek.greatsage.client.ClientPayloadHandler;
import dev.dedek.greatsage.client.voice.VoiceInput;
import dev.dedek.greatsage.network.AnalysisStatePayload;
import dev.dedek.greatsage.network.AnalyzeRequestPayload;
import dev.dedek.greatsage.network.AssessAuraPayload;
import dev.dedek.greatsage.network.AssessModePayload;
import dev.dedek.greatsage.network.AssessRequestPayload;
import dev.dedek.greatsage.network.AssessResultPayload;
import dev.dedek.greatsage.network.CeremonyPayload;
import dev.dedek.greatsage.network.EvolutionPayload;
import dev.dedek.greatsage.network.SkillAnnouncePayload;
import dev.dedek.greatsage.network.TipPayload;
import dev.dedek.greatsage.network.VoiceQueryPayload;
import dev.dedek.greatsage.network.VoiceReplyPayload;
import dev.dedek.greatsage.server.AnalysisManager;
import dev.dedek.greatsage.server.AssessService;
import dev.dedek.greatsage.server.AssessTracker;
import dev.dedek.greatsage.server.VoiceService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GreatSageNetwork {
    private GreatSageNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(SkillAnnouncePayload.TYPE, SkillAnnouncePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToClient(CeremonyPayload.TYPE, CeremonyPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToClient(EvolutionPayload.TYPE, EvolutionPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToClient(AnalysisStatePayload.TYPE, AnalysisStatePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToClient(AssessResultPayload.TYPE, AssessResultPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToClient(TipPayload.TYPE, TipPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToClient(AssessAuraPayload.TYPE, AssessAuraPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handle(payload)));
        registrar.playToServer(AssessModePayload.TYPE, AssessModePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                AssessTracker.setActive(p, payload.active());
            }
        }));
        registrar.playToServer(AssessRequestPayload.TYPE, AssessRequestPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                AssessService.assess(p, payload.entityId());
            }
        }));
        registrar.playToClient(VoiceReplyPayload.TYPE, VoiceReplyPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> VoiceInput.onReply(payload)));
        registrar.playToServer(VoiceQueryPayload.TYPE, VoiceQueryPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer p = (ServerPlayer)patt0$temp;
                VoiceService.handle(p, payload.command(), payload.argument(), payload.targetId());
            }
        }));
        registrar.playToServer(AnalyzeRequestPayload.TYPE, AnalyzeRequestPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)patt0$temp;
                if (payload.cancel()) {
                    AnalysisManager.cancel(player);
                } else {
                    AnalysisManager.start(player, payload.skillId());
                }
            }
        }));
    }
}

