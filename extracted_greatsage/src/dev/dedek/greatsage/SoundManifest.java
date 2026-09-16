/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package dev.dedek.greatsage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class SoundManifest {
    private static final Map<String, Integer> DURATIONS = SoundManifest.load();
    private static final Map<String, byte[]> ENVELOPES = SoundManifest.loadEnvelopes();

    private SoundManifest() {
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Map<String, Integer> load() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        try (InputStream in = SoundManifest.class.getResourceAsStream("/assets/greatsage/durations.json");){
            if (in == null) {
                Map<String, Integer> map2 = Collections.emptyMap();
                return map2;
            }
            JsonObject json = JsonParser.parseReader((Reader)new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Iterator iterator = json.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                map.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsInt());
            }
            return Collections.unmodifiableMap(map);
        }
        catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Map<String, byte[]> loadEnvelopes() {
        LinkedHashMap<String, byte[]> map = new LinkedHashMap<String, byte[]>();
        try (InputStream in = SoundManifest.class.getResourceAsStream("/assets/greatsage/envelopes.json");){
            if (in == null) {
                Map<String, byte[]> map2 = Collections.emptyMap();
                return map2;
            }
            JsonObject json = JsonParser.parseReader((Reader)new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Iterator iterator = json.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                String hex = ((JsonElement)entry.getValue()).getAsString();
                byte[] values = new byte[hex.length() / 2];
                for (int i = 0; i < values.length; ++i) {
                    values[i] = (byte)Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
                }
                map.put((String)entry.getKey(), values);
            }
            return Collections.unmodifiableMap(map);
        }
        catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static float amplitudeAt(String key, int tick) {
        byte[] values = ENVELOPES.get(key);
        if (values == null || tick < 0 || tick >= values.length) {
            return 0.0f;
        }
        return (float)values[tick] / 100.0f;
    }

    public static Set<String> keys() {
        return DURATIONS.keySet();
    }

    public static boolean has(String key) {
        return DURATIONS.containsKey(key);
    }

    public static int durationMs(String key) {
        Integer ticks = DURATIONS.get(key);
        return ticks == null ? 0 : ticks * 50;
    }
}

