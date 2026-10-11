package battleaudio.storage;

import battleaudio.model.Config;
import battleaudio.model.Config.Rule;
import battleaudio.model.Config.Candidate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public final class AudioLibrary {
    public static final String DECODE_ERROR = "音频格式不支持或文件已损坏，请更换文件";
    private final Path root;
    private final Path ffmpeg;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AudioLibrary(Path root, Path ffmpeg) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        this.ffmpeg = ffmpeg;
        Files.createDirectories(this.root.resolve("library"));
        Files.createDirectories(this.root.resolve("cache"));
    }

    public Path root() { return root; }

    public Config load() throws IOException {
        Path file = root.resolve("settings.json");
        if (!Files.exists(file)) EmbeddedPresets.install(this);
        if (!Files.exists(file)) return new Config();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Config config = Config.fromJson(new com.google.gson.JsonParser().parse(reader).getAsJsonObject());
            if (config == null) throw new IllegalArgumentException("音频配置为空");
            config.migrate();
            config.validate();
            for (Rule rule : config.rules) for (Candidate candidate : rule.candidates) {
                if (candidate.cachePath != null && !Files.isRegularFile(managed(candidate.cachePath)))
                    candidate.error = "播放缓存丢失，请更换文件";
            }
            return config;
        } catch (RuntimeException ex) {
            throw new IOException("无法读取音频设置；原文件已保留：" + ex.getMessage(), ex);
        }
    }

    public void save(Config config) throws IOException {
        config.validate();
        for (Rule rule : config.rules) for (Candidate candidate : rule.candidates) {
            if (candidate.sourcePath != null) managed(candidate.sourcePath);
            if (candidate.cachePath != null) managed(candidate.cachePath);
        }
        Path target = root.resolve("settings.json");
        Path temporary = Files.createTempFile(root, "settings-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { gson.toJson(config, writer); }
            if (Files.exists(target)) Files.copy(target, root.resolve("settings.json.bak"), StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public Rule importAudio(Path original) throws IOException {
        Rule rule = new Rule();
        rule.name = original.getFileName().toString();
        Candidate candidate = new Candidate();
        replace(candidate, original);
        rule.candidates.add(candidate);
        return rule;
    }

    public boolean replace(Rule rule, Path original) throws IOException {
        Candidate candidate = rule.audio();
        boolean reset = replace(candidate, original);
        return reset;
    }

    public boolean replace(Candidate candidate, Path original) throws IOException {
        candidate.name = original.getFileName().toString();
        String revision = UUID.randomUUID().toString();
        Path source = root.resolve("library").resolve(revision + ".source");
        Path cache = root.resolve("cache").resolve(revision + ".wav");
        Files.copy(original, source);
        candidate.sourcePath = root.relativize(source).toString();
        candidate.cachePath = null;
        candidate.error = null;
        boolean reset = false;
        try {
            decode(source, cache);
            double duration = duration(cache);
            if (duration <= 0) throw new IOException(DECODE_ERROR);
            reset = candidate.end <= candidate.start || candidate.end > duration || candidate.start >= duration;
            candidate.duration = duration;
            candidate.cachePath = root.relativize(cache).toString();
            if (reset) { candidate.start = 0; candidate.end = duration; }
        } catch (Exception ex) {
            Files.deleteIfExists(cache);
            candidate.duration = 0;
            candidate.error = DECODE_ERROR + "（" + ex.getMessage() + "）";
        }
        return reset;
    }


    private void decode(Path source, Path target) throws IOException, InterruptedException {
        if (ffmpeg == null || !Files.isRegularFile(ffmpeg)) throw new IOException("未找到本地 FFmpeg 解码程序");
        Path log = Files.createTempFile(root, "decode-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(ffmpeg.toString(), "-nostdin", "-v", "error", "-y",
                    "-protocol_whitelist", "file,pipe", "-i", source.toString(),
                    "-vn", "-map", "0:a:0", "-ac", "2", "-ar", "44100", "-c:a", "pcm_s16le", target.toString())
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("音频解码超时");
            }
            if (process.exitValue() != 0) throw new IOException(DECODE_ERROR);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(log);
        }
    }

    public static double duration(Path wav) throws Exception {
        try (java.io.InputStream input = new java.io.BufferedInputStream(Files.newInputStream(wav));
                AudioInputStream stream = AudioSystem.getAudioInputStream(input)) {
            return stream.getFrameLength() / (double) stream.getFormat().getFrameRate();
        }
    }

    public Path managed(String relative) {
        Path result = root.resolve(relative).normalize();
        if (Paths.get(relative).isAbsolute() || !result.startsWith(root)
                || !(result.startsWith(root.resolve("library")) || result.startsWith(root.resolve("cache"))))
            throw new IllegalArgumentException("音频库路径无效");
        return result;
    }

    public void collect(Config config, Set<String> activeCaches) throws IOException {
        Set<Path> keep = new HashSet<>();
        for (Rule rule : config.rules) for (Candidate candidate : rule.candidates) {
            if (candidate.sourcePath != null) keep.add(managed(candidate.sourcePath));
            if (candidate.cachePath != null) keep.add(managed(candidate.cachePath));
        }
        for (String cache : activeCaches) keep.add(managed(cache));
        for (String directory : new String[] {"library", "cache"}) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(root.resolve(directory))) {
                for (Path path : files) if (Files.isRegularFile(path) && !keep.contains(path)) Files.delete(path);
            }
        }
    }
}
