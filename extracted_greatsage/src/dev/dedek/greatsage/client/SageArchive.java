/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.reflect.TypeToken
 *  javax.annotation.Nullable
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ServerData
 *  net.minecraft.client.server.IntegratedServer
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.storage.LevelResource
 */
package dev.dedek.greatsage.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.dedek.greatsage.network.AssessResultPayload;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelResource;

public final class SageArchive {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "greatsage-observations.json";
    private static final String FOLDER = "greatsage-observations";
    private static final Map<String, Sighting> ENTRIES = new LinkedHashMap<String, Sighting>();
    private static String loadedKey = "";

    private SageArchive() {
    }

    public static void record(AssessResultPayload payload) {
        if (payload.failed() || payload.entityType().isEmpty()) {
            return;
        }
        SageArchive.load();
        Sighting sighting = ENTRIES.computeIfAbsent(SageArchive.keyFor(payload), key -> {
            Sighting fresh = new Sighting();
            int mark = key.indexOf(35);
            fresh.entityType = mark < 0 ? key : key.substring(0, mark);
            return fresh;
        });
        sighting.name = payload.name();
        if (!payload.race().isEmpty()) {
            sighting.race = payload.race();
        }
        sighting.epSum += payload.ep();
        ++sighting.epCount;
        sighting.maxHealth = Math.max(sighting.maxHealth, payload.maxHealth());
        if (!payload.skills().isEmpty()) {
            sighting.skills = new ArrayList<String>(payload.skills());
        }
        sighting.lastSeen = Util.getEpochMillis();
        SageArchive.save();
    }

    private static String keyFor(AssessResultPayload payload) {
        EntityType entityType;
        String type = payload.entityType();
        String name = payload.name();
        if (name.isEmpty()) {
            return type;
        }
        ResourceLocation id = ResourceLocation.tryParse((String)type);
        if (id != null && (entityType = (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null)) != null && entityType.getDescription().getString().equals(name)) {
            return type;
        }
        return type + "#" + name;
    }

    public static List<Sighting> all() {
        SageArchive.load();
        ArrayList<Sighting> out = new ArrayList<Sighting>(ENTRIES.values());
        out.sort(Comparator.comparingDouble(Sighting::averageEp).reversed());
        return out;
    }

    public static boolean isEmpty() {
        SageArchive.load();
        return ENTRIES.isEmpty();
    }

    private static String sanitize(String raw) {
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    @Nullable
    private static Path path() {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer local = minecraft.getSingleplayerServer();
        if (local != null) {
            return local.getWorldPath(LevelResource.ROOT).normalize().resolve(FILE);
        }
        ServerData server = minecraft.getCurrentServer();
        if (server == null) {
            return null;
        }
        return minecraft.gameDirectory.toPath().resolve(FOLDER).resolve(SageArchive.sanitize(server.ip) + ".json");
    }

    private static void adopt(Path file) {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer local = minecraft.getSingleplayerServer();
        if (local == null || Files.exists(file, new LinkOption[0])) {
            return;
        }
        Path legacy = minecraft.gameDirectory.toPath().resolve(FOLDER).resolve(SageArchive.sanitize(local.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString()) + ".json");
        if (!Files.isRegularFile(legacy, new LinkOption[0])) {
            return;
        }
        try {
            Files.move(legacy, file, new CopyOption[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static void load() {
        String key;
        Path file = SageArchive.path();
        String string = key = file == null ? "" : file.toAbsolutePath().toString();
        if (key.equals(loadedKey)) {
            return;
        }
        ENTRIES.clear();
        loadedKey = key;
        if (file == null) {
            return;
        }
        SageArchive.adopt(file);
        if (!Files.isRegularFile(file, new LinkOption[0])) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);){
            Map stored = (Map)GSON.fromJson((Reader)reader, new TypeToken<LinkedHashMap<String, Sighting>>(){}.getType());
            if (stored != null) {
                ENTRIES.putAll(stored);
            }
        }
        catch (IOException | RuntimeException failure) {
            ENTRIES.clear();
        }
    }

    private static void save() {
        Path file = SageArchive.path();
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
        }
        catch (IOException ignored) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, new OpenOption[0]);){
            GSON.toJson(ENTRIES, (Appendable)writer);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public static void unload() {
        ENTRIES.clear();
        loadedKey = "";
    }

    @Nullable
    public static Sighting byType(String entityType) {
        SageArchive.load();
        return ENTRIES.get(entityType);
    }

    public static final class Sighting {
        String entityType = "";
        String name = "";
        String race = "";
        double epSum;
        int epCount;
        float maxHealth;
        List<String> skills = new ArrayList<String>();
        long lastSeen;

        public String entityType() {
            return this.entityType;
        }

        public String name() {
            return this.name;
        }

        public String race() {
            return this.race;
        }

        public float maxHealth() {
            return this.maxHealth;
        }

        public int observations() {
            return this.epCount;
        }

        public long lastSeen() {
            return this.lastSeen;
        }

        public double averageEp() {
            return this.epCount <= 0 ? 0.0 : this.epSum / (double)this.epCount;
        }

        public Map<String, List<String>> byTier() {
            LinkedHashMap<String, List<String>> out = new LinkedHashMap<String, List<String>>();
            for (String raw : this.skills) {
                int split = raw.indexOf(124);
                String tier = split < 0 ? "unknown" : raw.substring(0, split);
                String label = split < 0 ? raw : raw.substring(split + 1);
                out.computeIfAbsent(tier, key -> new ArrayList()).add(label);
            }
            return out;
        }

        public int skillCount() {
            return this.skills.size();
        }
    }
}

