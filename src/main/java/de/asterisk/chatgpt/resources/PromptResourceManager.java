package de.asterisk.chatgpt.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PromptResourceManager {

    private static final Random random = new Random();
    private static final Map<String, String> cachedPrompts = new HashMap<>();
    private static final Map<String, String[]> cachedPromptVariants = new HashMap<>();

    public static String getPrompt(String key) {
        return cachedPrompts.computeIfAbsent(key, PromptResourceManager::loadPrompt);
    }

    public static String getPromptVariant(String key) {
        key = key + ".var";
        var variants = cachedPromptVariants.computeIfAbsent(key,  s -> {
            String content = loadPrompt(s);
            return Arrays.stream(content.split("\n")).filter(l -> !l.isBlank()).toArray(String[]::new);
        });
        if (variants.length == 0)
            throw new IllegalArgumentException("No prompt variant found for key: " + key);

        return variants[random.nextInt(variants.length - 1)];
    }

    private static String loadPrompt(String key) {
        try (InputStream stream = PromptResourceManager.class.getClassLoader().getResourceAsStream("prompts/" + key + ".txt")) {
            assert stream != null;
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            throw new RuntimeException("Failed to load prompt: " + key, exception);
        }
    }
}
