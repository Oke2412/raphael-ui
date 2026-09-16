/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.GreatSageConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class BattleSet {
    private BattleSet() {
    }

    public static List<String> all() {
        ArrayList<String> out = new ArrayList<String>();
        for (String entry : (List)GreatSageConfig.BATTLE_SKILLS.get()) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            out.add(trimmed);
        }
        return out;
    }

    public static boolean contains(ResourceLocation id) {
        return id != null && BattleSet.all().contains(id.toString());
    }

    public static int size() {
        return BattleSet.all().size();
    }

    public static void toggle(ResourceLocation id) {
        if (id == null) {
            return;
        }
        List<String> next = BattleSet.all();
        if (!next.remove(id.toString())) {
            next.add(id.toString());
        }
        GreatSageConfig.BATTLE_SKILLS.set(next);
        GreatSageConfig.SPEC.save();
    }

    public static void clear() {
        GreatSageConfig.BATTLE_SKILLS.set(List.of());
        GreatSageConfig.SPEC.save();
    }
}

