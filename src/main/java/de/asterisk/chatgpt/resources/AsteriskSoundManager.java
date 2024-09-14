package de.asterisk.chatgpt.resources;

import de.asterisk.chatgpt.utils.RandomStringGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AsteriskSoundManager {

    private static final String soundDirectory = System.getenv("ASTERISK_SOUNDS_DIR");
    private static final File soundCacheDir = new File(soundDirectory, "cache");

    public static File getCachedAudioFile(String name) {
        if (!soundCacheDir.exists() && !soundCacheDir.mkdirs()) {
            log.error("Failed to create sound cache directory: {}", soundCacheDir.getAbsolutePath());
            return null;
        }

        return new File(soundCacheDir, name);
    }

    public static SessionGuard createAudioSession() {
        return new SessionGuard();
    }

    public static class SessionGuard implements AutoCloseable{

        private final List<File> createdAudioFiles = new ArrayList<>();
        private final String sessionId;

        public SessionGuard() {
            this.sessionId = RandomStringGenerator.generateRandomString(16);
        }

        public File getAudioFile(String extension) {
            File audioFile = new File(soundDirectory, sessionId + "_" +  extension);
            createdAudioFiles.add(audioFile);
            return audioFile;
        }

        @Override
        public void close() {
            for (File audioFile : createdAudioFiles) {
                if (!audioFile.exists())
                    continue;

                log.debug("Deleting audio file: {}", audioFile.getAbsolutePath());
                if (!audioFile.delete()) {
                    log.error("Failed to delete audio file: {}", audioFile.getAbsolutePath());
                }
            }
        }
    }
}
