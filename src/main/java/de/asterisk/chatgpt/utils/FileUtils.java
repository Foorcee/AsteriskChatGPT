package de.asterisk.chatgpt.utils;

import com.google.common.io.Files;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Base64;

public class FileUtils {

    public static String readFileAsBase64(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    public static void writeBase64ToFile(String base64String, File outputFile) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(base64String);
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(decodedBytes);
        }
    }

    public static void convertAudioFile(File file) throws IOException {
        File tempFile = new File(file.getAbsolutePath() + ".tmp");

        // Temporäre Datei als AudioInputStream verwenden
        try (AudioInputStream originalStream = AudioSystem.getAudioInputStream(file)) {
            AudioFormat originalFormat = originalStream.getFormat();
            AudioFormat newFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    8000, // Neue Sampling-Rate
                    originalFormat.getSampleSizeInBits(),
                    originalFormat.getChannels(),
                    originalFormat.getFrameSize(),
                    8000, // Frame-Rate gleich der Sampling-Rate
                    originalFormat.isBigEndian()
            );

            // Audio-Daten in das neue Format umwandeln
            AudioInputStream newStream = AudioSystem.getAudioInputStream(newFormat, originalStream);

            AudioSystem.write(newStream, AudioFileFormat.Type.WAVE, tempFile);

            newStream.close();
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }

        Files.move(tempFile, file);
    }
}
