/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package dev.dedek.greatsage;

import java.util.Locale;
import javax.annotation.Nullable;

public enum CeremonyType {
    HERO_EGG("hero_egg", false),
    DEMON_LORD_SEED("demon_lord_seed", false),
    HARVEST_FESTIVAL("harvest_festival", false),
    TRUE_HERO("true_hero", true),
    TRUE_DEMON_LORD("true_demon_lord", true);

    private final String id;
    private final boolean listsSkills;

    private CeremonyType(String id, boolean listsSkills) {
        this.id = id;
        this.listsSkills = listsSkills;
    }

    public String id() {
        return this.id;
    }

    public boolean listsSkills() {
        return this.listsSkills;
    }

    public String soundKey() {
        return "ceremony." + this.id;
    }

    public String translationKey() {
        return "greatsage.ceremony." + this.id;
    }

    @Nullable
    public static CeremonyType byId(String id) {
        for (CeremonyType type : CeremonyType.values()) {
            if (!type.id.equalsIgnoreCase(id)) continue;
            return type;
        }
        return null;
    }

    public static boolean matches(String configEntry, CeremonyType type) {
        return configEntry.trim().toLowerCase(Locale.ROOT).equals(type.id);
    }
}

