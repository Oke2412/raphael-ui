/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package dev.dedek.greatsage;

import javax.annotation.Nullable;

public enum GreatSageTip {
    KOUSEKI_URA("kouseki_ura", 90, 0),
    KOUMYAKU_KENCHI("koumyaku_kenchi", 180, 0),
    KUUDOU_CHOKKA("kuudou_chokka", 120, 8),
    YOUGAN_KINSETSU("yougan_kinsetsu", 45, 0),
    SUIGEN_HAIGO("suigen_haigo", 90, 6),
    SEIMEI_TEIKA("seimei_teika", 40, 0),
    TEKISEI_FUKUSUU("tekisei_fukusuu", 60, 0),
    RAKKA_KIKEN("rakka_kiken", 30, 0),
    SANSO_KINSHOU("sanso_kinshou", 30, 0),
    NENSHOU_KAKUNIN("nenshou_kakunin", 40, 0),
    DOKUSO_JUNKAN("dokuso_junkan", 60, 0),
    HAIGO_SEKKIN("haigo_sekkin", 45, 0),
    SHINKA_SEKKIN("shinka_sekkin", 600, 0),
    SHUUJUKU_JOUGEN("shuujuku_jougen", 300, 0),
    MIKAISEKI_SONZAI("mikaiseki_sonzai", 1800, 4),
    MICHI_RYOUIKI("michi_ryouiki", 60, 0),
    MASO_NOUDO("maso_noudo", 600, 0),
    NICHIBOTSU("nichibotsu", 900, 5),
    KISHOU_KYUUHEN("kishou_kyuuhen", 600, 6),
    KOURYOU_FUSOKU("kouryou_fusoku", 240, 6),
    SHUUNOU_JOUGEN("shuunou_jougen", 120, 0),
    DOUGU_HASON("dougu_hason", 90, 0),
    TEKISHITA_DOUGU("tekishita_dougu", 240, 6),
    IBUKURO_WAZUKA("ibukuro_wazuka", 300, 0),
    KATSUDOU_TEISHI("katsudou_teishi", 0, 0),
    SAIKOUSEI("saikousei", 0, 0),
    KYUUSOKU_SUISHOU("kyuusoku_suishou", 1200, 4),
    RENZOKU_KATSUDOU("renzoku_katsudou", 3600, 3),
    HOUKOKU_NASHI("houkoku_nashi", 1800, 3),
    HANDAN_SHIJI("handan_shiji", 1800, 3);

    private final String id;
    private final int cooldownSeconds;
    private final int maxTimes;

    private GreatSageTip(String id, int cooldownSeconds, int maxTimes) {
        this.id = id;
        this.cooldownSeconds = cooldownSeconds;
        this.maxTimes = maxTimes;
    }

    public String id() {
        return this.id;
    }

    public int cooldownTicks() {
        return this.cooldownSeconds * 20;
    }

    public int maxTimes() {
        return this.maxTimes;
    }

    public String soundKey() {
        return "chat." + this.id;
    }

    public String translationKey() {
        return "greatsage.tip." + this.id;
    }

    @Nullable
    public static GreatSageTip byId(String id) {
        for (GreatSageTip tip : GreatSageTip.values()) {
            if (!tip.id.equals(id)) continue;
            return tip;
        }
        return null;
    }
}

