package de.asterisk.chatgpt.scripts;

import com.theokanning.openai.audio.CreateSpeechRequest;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.audio.TranscriptionResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import de.asterisk.chatgpt.utils.Constants;
import de.asterisk.chatgpt.utils.OpenAIVoice;
import okhttp3.ResponseBody;
import org.asteriskjava.fastagi.BaseAgiScript;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractChatGTPScript extends BaseAgiScript {

    protected static final String AUDIO_FORMAT = "wav";

    private final List<ChatMessage> messages = new ArrayList<>();
    private final OpenAiService openAiService;

    public AbstractChatGTPScript() {
        String apiKey = Constants.OPEN_AI_API_KEY;
        if (apiKey == null)
            throw new IllegalStateException("OpenAI API key is not set");

        this.openAiService = new OpenAiService(apiKey);
    }

    public void addSystemMessage(String text) {
        addMessage(ChatMessageRole.SYSTEM, text);
    }

    public void addAssistantMessage(String text) {
        addMessage(ChatMessageRole.ASSISTANT, text);
    }

    public void addUserMessage(String text) {
        addMessage(ChatMessageRole.USER, text);
    }

    private void addMessage(ChatMessageRole role, String message) {
        messages.add(new ChatMessage(role.value(), message));
    }

    public String getChatResponse() {
        var request = ChatCompletionRequest.builder().model(Constants.DEFAULT_MODEL)
                        .messages(messages).build();

        var response = openAiService.createChatCompletion(request);
        if (response == null)
            throw new IllegalStateException("OpenAI response is null");

        return response.getChoices().getFirst().getMessage().getContent();
    }

    public void getTextToSpeech(String text, File file) {
        CreateSpeechRequest request = CreateSpeechRequest.builder()
                .input(text).model(OpenAIVoice.Model).voice(OpenAIVoice.NOVA)
                .responseFormat(AUDIO_FORMAT).build();

        try (ResponseBody response = openAiService.createSpeech(request)) {
            // Temporäre Datei erstellen
            File tempFile = File.createTempFile("tts", ".tmp");

            try (InputStream inputStream = response.byteStream();
                 FileOutputStream tempFileOutStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    tempFileOutStream.write(buffer, 0, bytesRead);
                }
            }

            // Temporäre Datei als AudioInputStream verwenden
            try (AudioInputStream originalStream = AudioSystem.getAudioInputStream(tempFile)) {

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

                AudioSystem.write(newStream, AudioFileFormat.Type.WAVE, file);

                newStream.close();
            }

            tempFile.delete();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save text to speech", exception);
        } catch (UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSpeechToText(File file) {
        CreateTranscriptionRequest request = CreateTranscriptionRequest.builder()
                .language("de").model("whisper-1").build();

        TranscriptionResult result = openAiService.createTranscription(request, file);
        return result.getText();
    }
}
