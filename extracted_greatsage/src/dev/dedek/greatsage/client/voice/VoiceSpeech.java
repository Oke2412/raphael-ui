/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package dev.dedek.greatsage.client.voice;

import dev.dedek.greatsage.GreatSageConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoiceSpeech {
    private static final Logger LOG = LoggerFactory.getLogger((String)"greatsage/voice");
    private static final String FOLDER = "greatsage-voice";
    private static final AudioFormat FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);
    private static Status status = Status.NO_LIBRARY;
    private static long probedAt;
    private static final long RETRY_MS = 5000L;
    private static Class<?> modelClass;
    private static Class<?> recognizerClass;
    private static Object model;
    private static Object recognizer;
    private static volatile Thread pump;
    private static TargetDataLine line;
    private static final AtomicBoolean RUNNING;
    private static final ConcurrentLinkedQueue<String> HEARD;
    private static final AtomicReference<String> PARTIAL;

    private VoiceSpeech() {
    }

    public static Status status() {
        VoiceSpeech.probe();
        return status;
    }

    public static boolean available() {
        return VoiceSpeech.status() == Status.READY;
    }

    public static Path home() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER);
    }

    public static String describe() {
        return VoiceSpeech.home().toAbsolutePath().toString();
    }

    private static synchronized void probe() {
        long now = System.currentTimeMillis();
        if (status == Status.READY || probedAt != 0L && now - probedAt < 5000L) {
            return;
        }
        probedAt = now;
        Path lib = VoiceSpeech.home().resolve("lib");
        Path modelPath = VoiceSpeech.home().resolve("model");
        try {
            if (!Files.isDirectory(lib, new LinkOption[0])) {
                status = Status.NO_LIBRARY;
                return;
            }
            ArrayList<URL> jars = new ArrayList<URL>();
            try (Stream<Path> stream = Files.list(lib);){
                for (Path entry : stream.toList()) {
                    if (!entry.toString().toLowerCase().endsWith(".jar")) continue;
                    jars.add(entry.toUri().toURL());
                }
            }
            if (jars.isEmpty()) {
                status = Status.NO_LIBRARY;
                return;
            }
            if (!Files.isDirectory(modelPath, new LinkOption[0])) {
                status = Status.NO_MODEL;
                return;
            }
            URLClassLoader loader = new URLClassLoader(jars.toArray(new URL[0]), VoiceSpeech.class.getClassLoader());
            modelClass = Class.forName("org.vosk.Model", true, loader);
            recognizerClass = Class.forName("org.vosk.Recognizer", true, loader);
            model = modelClass.getConstructor(String.class).newInstance(modelPath.toAbsolutePath().toString());
            status = Status.READY;
            LOG.info("[greatsage] reconnaissance vocale prete, modele : {}", (Object)modelPath);
        }
        catch (Throwable failure) {
            status = Status.FAILED;
            LOG.warn("[greatsage] reconnaissance vocale indisponible : {}", (Object)failure.toString());
        }
    }

    public static synchronized boolean open(String grammar) {
        VoiceSpeech.probe();
        if (status != Status.READY || RUNNING.get()) {
            return RUNNING.get();
        }
        try {
            Constructor<?> build = recognizerClass.getConstructor(modelClass, Float.TYPE, String.class);
            recognizer = build.newInstance(model, Float.valueOf(16000.0f), grammar);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                status = Status.NO_MICROPHONE;
                return false;
            }
            TargetDataLine opened = (TargetDataLine)AudioSystem.getLine(info);
            opened.open(FORMAT);
            opened.start();
            line = opened;
            RUNNING.set(true);
            HEARD.clear();
            PARTIAL.set("");
            Thread worker = new Thread(() -> VoiceSpeech.listen(opened), FOLDER);
            worker.setDaemon(true);
            pump = worker;
            worker.start();
            return true;
        }
        catch (Throwable failure) {
            LOG.warn("[greatsage] micro indisponible : {}", (Object)failure.toString());
            status = Status.NO_MICROPHONE;
            VoiceSpeech.close();
            return false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void listen(TargetDataLine source) {
        byte[] buffer = new byte[2048];
        try {
            Method accept = recognizerClass.getMethod("acceptWaveForm", byte[].class, Integer.TYPE);
            Method result = recognizerClass.getMethod("getResult", new Class[0]);
            Method partial = recognizerClass.getMethod("getPartialResult", new Class[0]);
            while (RUNNING.get()) {
                Object active = recognizer;
                if (active == null) {
                    break;
                }
                int read = source.read(buffer, 0, buffer.length);
                if (read <= 0) continue;
                if (Boolean.TRUE.equals(accept.invoke(active, buffer, read))) {
                    String json = String.valueOf(result.invoke(active, new Object[0]));
                    String text = VoiceSpeech.extract(json, "text");
                    PARTIAL.set("");
                    double conf = VoiceSpeech.confidence(json);
                    if (!text.isBlank() && conf >= VoiceSpeech.threshold()) {
                        HEARD.add(text);
                        continue;
                    }
                    if (text.isBlank()) continue;
                    LOG.info("[greatsage] rejete \"{}\" (confiance {})", (Object)text, (Object)String.format(Locale.ROOT, "%.2f", conf));
                    continue;
                }
                PARTIAL.set(VoiceSpeech.extract(String.valueOf(partial.invoke(active, new Object[0])), "partial"));
            }
        }
        catch (Throwable failure) {
            LOG.warn("[greatsage] ecoute interrompue : {}", (Object)failure.toString());
        }
        finally {
            RUNNING.set(false);
        }
    }

    private static double confidence(String json) {
        double sum = 0.0;
        int count = 0;
        int at = 0;
        while ((at = json.indexOf("\"conf\"", at)) >= 0) {
            int colon;
            int end;
            for (end = colon = json.indexOf(58, at); end < json.length() && ",}] ".indexOf(json.charAt(end)) < 0; ++end) {
            }
            try {
                sum += Double.parseDouble(json.substring(colon + 1, end).trim());
                ++count;
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            at = end;
        }
        return count == 0 ? 1.0 : sum / (double)count;
    }

    private static double threshold() {
        return (Double)GreatSageConfig.VOICE_CONFIDENCE.get();
    }

    private static String extract(String json, String field) {
        int key = json.indexOf("\"" + field + "\"");
        if (key < 0) {
            return "";
        }
        int open = json.indexOf(34, json.indexOf(58, key) + 1);
        int close = open < 0 ? -1 : json.indexOf(34, open + 1);
        return open < 0 || close < 0 ? "" : json.substring(open + 1, close).trim();
    }

    @Nullable
    public static String poll() {
        return HEARD.poll();
    }

    public static String partial() {
        return PARTIAL.get();
    }

    public static boolean listening() {
        return RUNNING.get();
    }

    public static synchronized void close() {
        RUNNING.set(false);
        TargetDataLine current = line;
        line = null;
        if (current != null) {
            try {
                current.stop();
                current.close();
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
        }
        Thread worker = pump;
        pump = null;
        if (worker != null && worker != Thread.currentThread()) {
            try {
                worker.join(1500L);
            }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            if (worker.isAlive()) {
                LOG.warn("[greatsage] la capture ne s'est pas arretee, le reconnaisseur n'est pas libere");
                recognizer = null;
                HEARD.clear();
                PARTIAL.set("");
                return;
            }
        }
        if (recognizer != null) {
            try {
                recognizerClass.getMethod("close", new Class[0]).invoke(recognizer, new Object[0]);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            recognizer = null;
        }
        HEARD.clear();
        PARTIAL.set("");
    }

    static {
        RUNNING = new AtomicBoolean();
        HEARD = new ConcurrentLinkedQueue();
        PARTIAL = new AtomicReference<String>("");
    }

    public static enum Status {
        READY,
        NO_LIBRARY,
        NO_MODEL,
        NO_MICROPHONE,
        FAILED;

    }
}

