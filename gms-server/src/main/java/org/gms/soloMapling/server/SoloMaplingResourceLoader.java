package org.gms.soloMapling.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized resource loader for SoloMapling.
 * Resolves configuration YAMLs, movement recording files (.bin, .csv), dialogue packs,
 * and text files from classpath resources or filesystem.
 */
public class SoloMaplingResourceLoader {

    private static final Logger log = LoggerFactory.getLogger(SoloMaplingResourceLoader.class);

    private static final String BASE_RESOURCE_DIR = "soloMapling/";
    private static final List<Path> SEARCH_DIRS = new ArrayList<>();

    static {
        // Standard resources path
        SEARCH_DIRS.add(Paths.get("src", "main", "resources", "soloMapling"));
        SEARCH_DIRS.add(Paths.get("target", "classes", "soloMapling"));
        SEARCH_DIRS.add(Paths.get("resources", "soloMapling"));
        SEARCH_DIRS.add(Paths.get("soloMapling"));
        SEARCH_DIRS.add(Paths.get("src", "main", "java", "soloMapling"));
        SEARCH_DIRS.add(Paths.get("SoloMapling-0.3", "src", "main", "java", "soloMapling"));
    }

    /**
     * Normalizes a legacy path string (e.g. "src/main/resources/soloMapling/ArtificialPlayer/BotDialoguePack/foo.yaml")
     * into a relative subpath (e.g. "BotDialoguePack/foo.yaml").
     */
    public static String normalizePath(String rawPath) {
        if (rawPath == null) {
            return "";
        }
        String p = rawPath.replace('\\', '/').trim();

        // Strip known prefixes
        String[] prefixes = {
                "src/main/resources/soloMapling/ArtificialPlayer/BotMovementSystem/",
                "src/main/resources/soloMapling/ArtificialPlayer/",
                "src/main/resources/soloMapling/FreeMarket/",
                "src/main/resources/soloMapling/",
                "src/main/resources/soloMapling/",
                "soloMapling/ArtificialPlayer/BotMovementSystem/",
                "soloMapling/ArtificialPlayer/",
                "soloMapling/FreeMarket/",
                "soloMapling/"
        };

        for (String prefix : prefixes) {
            if (p.startsWith(prefix)) {
                return p.substring(prefix.length());
            }
        }
        return p;
    }

    /**
     * Resolves a relative or legacy path to an existing filesystem Path.
     */
    public static Path resolvePath(String rawPath) {
        String normalized = normalizePath(rawPath);

        // 1. Direct file check on rawPath if exists
        Path direct = Paths.get(rawPath);
        if (Files.exists(direct)) {
            return direct;
        }

        // 2. Check search directories with normalized path
        for (Path base : SEARCH_DIRS) {
            Path candidate = base.resolve(normalized);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        // 3. Check search directories with rawPath
        for (Path base : SEARCH_DIRS) {
            Path candidate = base.resolve(rawPath.replace('\\', '/'));
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        // Fallback: return preferred destination path under src/main/resources/soloMapling
        return SEARCH_DIRS.get(0).resolve(normalized);
    }

    /**
     * Resolves a directory path as a File object.
     */
    public static File resolveDirectory(String rawPath) {
        return resolvePath(rawPath).toFile();
    }

    /**
     * Obtains an InputStream for the given resource path.
     * Tries ClassLoader first, then falls back to resolved filesystem path.
     */
    public static InputStream getInputStream(String rawPath) {
        String normalized = normalizePath(rawPath);

        // Try ClassLoader
        ClassLoader cl = SoloMaplingResourceLoader.class.getClassLoader();
        InputStream is = cl.getResourceAsStream(BASE_RESOURCE_DIR + normalized);
        if (is != null) {
            return is;
        }
        is = cl.getResourceAsStream(normalized);
        if (is != null) {
            return is;
        }

        // Try filesystem
        Path resolved = resolvePath(rawPath);
        if (Files.exists(resolved) && !Files.isDirectory(resolved)) {
            try {
                return Files.newInputStream(resolved);
            } catch (IOException e) {
                log.error("Failed to open InputStream for path: {}", resolved, e);
            }
        }

        log.warn("Resource not found: {} (normalized: {})", rawPath, normalized);
        return null;
    }

    /**
     * Obtains a Reader for the given resource path (UTF-8).
     */
    public static Reader getReader(String rawPath) {
        InputStream is = getInputStream(rawPath);
        if (is == null) {
            Path resolved = resolvePath(rawPath);
            try {
                return new FileReader(resolved.toFile(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Failed to create Reader for: {}", rawPath, e);
                return new StringReader("");
            }
        }
        return new InputStreamReader(is, StandardCharsets.UTF_8);
    }

    /**
     * Reads all lines from a resource file.
     */
    public static List<String> readAllLines(String rawPath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(getReader(rawPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            log.error("Failed reading lines from: {}", rawPath, e);
        }
        return lines;
    }
}
