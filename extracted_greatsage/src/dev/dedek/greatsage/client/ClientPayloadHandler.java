/*
 * Decompiled with CFR 0.152.
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.CeremonyType;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.client.AnalysisClient;
import dev.dedek.greatsage.client.Announcement;
import dev.dedek.greatsage.client.AnnouncementManager;
import dev.dedek.greatsage.client.AssessMode;
import dev.dedek.greatsage.client.SageTips;
import dev.dedek.greatsage.network.AnalysisStatePayload;
import dev.dedek.greatsage.network.AssessAuraPayload;
import dev.dedek.greatsage.network.AssessResultPayload;
import dev.dedek.greatsage.network.CeremonyPayload;
import dev.dedek.greatsage.network.EvolutionPayload;
import dev.dedek.greatsage.network.SkillAnnouncePayload;
import dev.dedek.greatsage.network.TipPayload;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handle(SkillAnnouncePayload payload) {
        if (GreatSageConfig.blacklisted(payload.skillId().toString())) {
            return;
        }
        if (!GreatSageConfig.tierAnnounced(payload.tier())) {
            return;
        }
        AnnouncementManager.submit(new Announcement.Entry(payload.skillId(), payload.tier(), payload.displayName()));
    }

    public static void handle(EvolutionPayload payload) {
        AnnouncementManager.startEvolution(new Announcement.Evolution(payload.fromId(), payload.fromName(), payload.toId(), payload.toName()));
    }

    public static void handle(CeremonyPayload payload) {
        CeremonyType type = CeremonyType.byId(payload.ceremonyId());
        if (type != null) {
            AnnouncementManager.startCeremony(type);
        }
    }

    public static void handle(AnalysisStatePayload payload) {
        AnalysisClient.accept(payload);
    }

    public static void handle(TipPayload payload) {
        SageTips.accept(payload.tip());
    }

    public static void handle(AssessAuraPayload payload) {
        AssessMode.accept(payload);
    }

    public static void handle(AssessResultPayload payload) {
        AssessMode.accept(payload);
    }
}

