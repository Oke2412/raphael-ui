/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.network.chat.Component
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package dev.dedek.greatsage.client.voice;

import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.client.voice.VoiceSpeech;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoiceInstall {
    private static final Logger LOG = LoggerFactory.getLogger((String)"greatsage/voice");
    private static final List<Piece> LIBRARY = List.of(new Piece("vosk-0.3.45.jar", "https://repo1.maven.org/maven2/com/alphacephei/vosk/0.3.45/vosk-0.3.45.jar", "9e38e96e4448a41d889bb254b2f8424945554945c6e77e1cd97a3d8633fac2ba", 25861916L), new Piece("jna-5.13.0.jar", "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar", "66d4f819a062a51a1d5627bffc23fac55d1677f0e0a1feba144aabdd670a64bb", 1879325L));
    private static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
    private static final long MODEL_SIZE = 41205931L;
    private static final List<String> MODEL_PARTS = List.of("am", "conf", "graph");
    private static final long MODEL_CEILING = 0x19000000L;
    private static volatile Phase phase = Phase.IDLE;
    private static volatile String detail = "";
    private static volatile int percent;
    private static volatile Thread worker;
    private static volatile int reported;
    private static final Deque<Component> PENDING;

    private VoiceInstall() {
    }

    public static Phase phase() {
        return phase;
    }

    public static String detail() {
        return detail;
    }

    public static int percent() {
        return percent;
    }

    public static void ensure() {
        boolean needModel;
        boolean allowed;
        boolean wanted;
        if (phase != Phase.IDLE) {
            return;
        }
        try {
            wanted = !"off".equalsIgnoreCase((String)GreatSageConfig.VOICE_INPUT.get());
            allowed = (Boolean)GreatSageConfig.VOICE_AUTO_DOWNLOAD.get();
        }
        catch (Throwable notReadyYet) {
            return;
        }
        if (!wanted || !allowed) {
            return;
        }
        Path home = VoiceSpeech.home();
        boolean needLibrary = !VoiceInstall.hasLibrary(home);
        boolean bl = needModel = !VoiceInstall.hasModel(home.resolve("model"));
        if (!needLibrary && !needModel) {
            phase = Phase.DONE;
            return;
        }
        phase = Phase.WORKING;
        percent = 0;
        detail = "";
        Thread thread = new Thread(() -> VoiceInstall.install(home, needLibrary, needModel), "greatsage-voice-install");
        thread.setDaemon(true);
        worker = thread;
        thread.start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void drain(Minecraft minecraft) {
        if (PENDING.isEmpty() || minecraft.player == null) {
            return;
        }
        Deque<Component> deque = PENDING;
        synchronized (deque) {
            while (!PENDING.isEmpty()) {
                minecraft.player.displayClientMessage(PENDING.poll(), false);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void tell(Component message) {
        Deque<Component> deque = PENDING;
        synchronized (deque) {
            PENDING.add(message);
        }
    }

    private static boolean hasLibrary(Path home) {
        boolean bl;
        block9: {
            Path lib = home.resolve("lib");
            if (!Files.isDirectory(lib, new LinkOption[0])) {
                return false;
            }
            Stream<Path> stream = Files.list(lib);
            try {
                bl = stream.anyMatch(entry -> entry.toString().toLowerCase().endsWith(".jar"));
                if (stream == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (stream != null) {
                        try {
                            stream.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException unreadable) {
                    return false;
                }
            }
            stream.close();
        }
        return bl;
    }

    private static boolean hasModel(Path model) {
        if (!Files.isDirectory(model, new LinkOption[0])) {
            return false;
        }
        for (String part : MODEL_PARTS) {
            if (Files.isDirectory(model.resolve(part), new LinkOption[0])) continue;
            return false;
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void install(Path home, boolean needLibrary, boolean needModel) {
        Path work = home.resolve("download");
        try {
            Files.createDirectories(home.resolve("lib"), new FileAttribute[0]);
            Files.createDirectories(work, new FileAttribute[0]);
            LOG.info("[greatsage] installation de la reconnaissance vocale dans {}", (Object)home);
            VoiceInstall.tell((Component)Component.translatable((String)"greatsage.voice.install.start"));
            if (needLibrary) {
                for (Piece piece : LIBRARY) {
                    Path target = home.resolve("lib").resolve(piece.name());
                    if (Files.isRegularFile(target, new LinkOption[0]) && piece.sha256().equals(VoiceInstall.digest(target))) continue;
                    Path temp = work.resolve(piece.name() + ".part");
                    detail = piece.name();
                    VoiceInstall.fetch(piece.url(), temp, piece.size());
                    String seen = VoiceInstall.digest(temp);
                    if (!piece.sha256().equals(seen)) {
                        Files.deleteIfExists(temp);
                        throw new IOException("empreinte inattendue pour " + piece.name() + " : " + seen);
                    }
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                    LOG.info("[greatsage] {} verifie et installe", (Object)piece.name());
                }
            }
            if (needModel) {
                detail = "model";
                Path archive = work.resolve("model.zip");
                VoiceInstall.fetch(MODEL_URL, archive, 41205931L);
                Path staging = work.resolve("staging");
                VoiceInstall.wipe(staging);
                Files.createDirectories(staging, new FileAttribute[0]);
                VoiceInstall.unzip(archive, staging);
                Path root = VoiceInstall.modelRoot(staging);
                if (root == null || !VoiceInstall.hasModel(root)) {
                    throw new IOException("l'archive ne contient pas un modele Vosk");
                }
                Path target = home.resolve("model");
                VoiceInstall.wipe(target);
                Files.move(root, target, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(archive);
                LOG.info("[greatsage] modele de langue installe dans {}", (Object)target);
                percent = 100;
            }
            VoiceInstall.wipe(work);
            percent = 100;
            phase = Phase.DONE;
            detail = "";
            VoiceInstall.tell((Component)Component.translatable((String)"greatsage.voice.install.done"));
        }
        catch (Throwable failure) {
            phase = Phase.FAILED;
            detail = failure.toString();
            LOG.warn("[greatsage] installation de la voix echouee : {}", (Object)detail);
            VoiceInstall.tell((Component)Component.translatable((String)"greatsage.voice.install.failed", (Object[])new Object[]{VoiceSpeech.describe()}));
            try {
                VoiceInstall.wipe(work.resolve("staging"));
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        finally {
            worker = null;
        }
    }

    private static void fetch(String address, Path to, long expected) throws IOException {
        OpenOption[] openOptionArray;
        HttpURLConnection http;
        boolean resumed;
        URI uri = URI.create(address);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("adresse non chiffree refusee : " + address);
        }
        Files.createDirectories(to.getParent(), new FileAttribute[0]);
        long already = 0L;
        if (expected > 0L && Files.isRegularFile(to, new LinkOption[0])) {
            long onDisk = Files.size(to);
            if (onDisk == expected) {
                return;
            }
            if (onDisk < expected) {
                already = onDisk;
            } else {
                Files.delete(to);
            }
        }
        URLConnection connection = uri.toURL().openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "GreatSage");
        if (already > 0L) {
            connection.setRequestProperty("Range", "bytes=" + already + "-");
        }
        boolean bl = resumed = already > 0L && connection instanceof HttpURLConnection && (http = (HttpURLConnection)connection).getResponseCode() == 206;
        if (already > 0L && !resumed) {
            already = 0L;
        }
        if (already > 0L) {
            LOG.info("[greatsage] reprise du telechargement a {} octets", (Object)already);
        }
        long ceiling = expected > 0L ? expected : 0x19000000L;
        AtomicLong got = new AtomicLong(already);
        if (resumed) {
            OpenOption[] openOptionArray2 = new OpenOption[3];
            openOptionArray2[0] = StandardOpenOption.CREATE;
            openOptionArray2[1] = StandardOpenOption.WRITE;
            openOptionArray = openOptionArray2;
            openOptionArray2[2] = StandardOpenOption.APPEND;
        } else {
            OpenOption[] openOptionArray3 = new OpenOption[3];
            openOptionArray3[0] = StandardOpenOption.CREATE;
            openOptionArray3[1] = StandardOpenOption.WRITE;
            openOptionArray = openOptionArray3;
            openOptionArray3[2] = StandardOpenOption.TRUNCATE_EXISTING;
        }
        OpenOption[] mode = openOptionArray;
        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(to, mode);){
            int read;
            byte[] buffer = new byte[65536];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
                long total = got.addAndGet(read);
                if (total > ceiling) {
                    throw new IOException("reponse plus grosse que prevu pour " + address);
                }
                if (expected <= 0L) continue;
                VoiceInstall.report((int)Math.min(99L, total * 100L / expected));
            }
        }
        if (expected > 0L && got.get() != expected) {
            throw new IOException("taille inattendue pour " + address + " : " + got.get() + " au lieu de " + expected);
        }
    }

    private static void report(int now) {
        percent = now;
        int quarter = now / 25;
        if (quarter <= reported || quarter >= 4) {
            return;
        }
        reported = quarter;
        VoiceInstall.tell((Component)Component.translatable((String)"greatsage.voice.install.progress", (Object[])new Object[]{quarter * 25}));
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static void unzip(Path archive, Path into) throws IOException {
        Path base = into.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive, new OpenOption[0]));){
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = base.resolve(entry.getName()).normalize();
                if (!target.startsWith(base)) {
                    throw new IOException("entree hors du dossier : " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target, new FileAttribute[0]);
                    continue;
                }
                Files.createDirectories(target.getParent(), new FileAttribute[0]);
                OutputStream out = Files.newOutputStream(target, new OpenOption[0]);
                try {
                    zip.transferTo(out);
                }
                finally {
                    if (out == null) continue;
                    out.close();
                }
            }
            return;
        }
    }

    private static Path modelRoot(Path staging) throws IOException {
        if (VoiceInstall.hasModel(staging)) {
            return staging;
        }
        try (Stream<Path> stream = Files.list(staging);){
            ArrayList<Path> entries = new ArrayList<Path>(stream.toList());
            for (Path entry : entries) {
                if (!Files.isDirectory(entry, new LinkOption[0]) || !VoiceInstall.hasModel(entry)) continue;
                Path path = entry;
                return path;
            }
        }
        return null;
    }

    private static void wipe(Path path) throws IOException {
        if (!Files.exists(path, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path, new FileVisitOption[0]);){
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private static String digest(Path file) throws IOException {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file, new OpenOption[0]);){
                int read;
                byte[] buffer = new byte[65536];
                while ((read = in.read(buffer)) > 0) {
                    sha.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(sha.digest());
        }
        catch (NoSuchAlgorithmException impossible) {
            throw new IOException(impossible);
        }
    }

    static {
        PENDING = new ArrayDeque<Component>();
    }

    public static enum Phase {
        IDLE,
        WORKING,
        DONE,
        FAILED;

    }

    private record Piece(String name, String url, String sha256, long size) {
    }
}

