package de.asterisk.chatgpt.scripts;

import de.asterisk.chatgpt.resources.AsteriskSoundManager;
import de.asterisk.chatgpt.resources.PromptResourceManager;
import io.reactivex.Completable;
import lombok.extern.slf4j.Slf4j;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class DefaultChatGPTScript extends AbstractChatGTPScript {

    private static final String HANGUP = "[HANGUP]";

    private final String welcomeMessage;

    public DefaultChatGPTScript() {
        this.welcomeMessage = PromptResourceManager.getPrompt("welcome_message");
        addAssistantMessage(welcomeMessage);
    }

    @Override
    public void service(AgiRequest agiRequest, AgiChannel agiChannel) throws AgiException {
        String callerId = agiRequest.getCallerIdNumber();
        log.info("Received call from: {}", callerId);

        try {
            //Wir müssen hier warten, da sonst die Audio knallt
            Thread.sleep(1300L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //Play welcome message
        File welcomeFile = getWelcomeAudioFile();
        playAudioFile(welcomeFile);

        while (true) {
            try (AsteriskSoundManager.SessionGuard session = AsteriskSoundManager.createAudioSession()) {
                //Record the call
                File recording = session.getAudioFile("input.wav");
                recordFile(recording.getAbsolutePath().replace(".wav",""), AUDIO_FORMAT, "#", 10000, 0, true, 2);
                log.info("Recording saved to: {}", recording.getAbsolutePath());

                //Transcribe the recording
                String heardText = getSpeechToText(recording);
                log.info("Heard: {}", heardText);

                if (!hasUnderstood(heardText)) {
                    String fallbackText = PromptResourceManager.getPromptVariant("fallback_message");
                    addAssistantMessage(fallbackText);
                    File file = getCachedTextToSpeech("fallback", fallbackText);
                    playAudioFile(file);
                    continue;
                }

                onHeardText(heardText);
                addUserMessage(heardText);

                CompletableFuture<AsyncResponse> asyncResponse = CompletableFuture.supplyAsync(() -> {
                    //Get response from assistant
                    String assistantResponse = getChatResponse();

                    log.info("Assistant response: {}", assistantResponse);

                    boolean isHangup = assistantResponse.endsWith(HANGUP);
                    assistantResponse = cleanText(assistantResponse);

                    addAssistantMessage(assistantResponse);

                    log.info("Converting text to speech...");
                    File responseFile = session.getAudioFile("response.wav");
                    getTextToSpeech(assistantResponse, responseFile);

                    return new AsyncResponse(responseFile, isHangup);
                });

                if (heardText.length() > 200) {
                    String fillMessage = PromptResourceManager.getPromptVariant("fill_message");
                    File fillFile = getCachedTextToSpeech("fill", fillMessage);
                    playAudioFile(fillFile);
                }

                AsyncResponse response = asyncResponse.get();

                log.info("Playing response...");
                playAudioFile(response.responseFile());

                if (response.hangup()) {
                    log.info("Call ended by assistant");
                    hangup();
                    return;
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void init() {
        String systemPrompt = PromptResourceManager.getPrompt("default_system_prompt");
        addSystemMessage(systemPrompt);
    }

    public void onHeardText(String text) {

    }

    private File getCachedTextToSpeech(String type, String text) {
        File audioFile = AsteriskSoundManager.getCachedAudioFile(type + "_" + text.hashCode() + ".wav");
        if (audioFile != null && audioFile.exists()) {
            log.info("Using cached {} message {}", type, audioFile);
            return audioFile;
        }

        log.info("Generating {} message audio file {}", type, audioFile);
        getTextToSpeech(text, audioFile);
        return audioFile;
    }

    private File getWelcomeAudioFile() {
        return getCachedTextToSpeech("welcome", welcomeMessage);
    }

    private void playAudioFile(File file) throws AgiException {
        String fileName = file.getAbsolutePath().replace(".wav", "");
        streamFile(fileName);
    }

    private String cleanText(String text) {
        return text.replace("\n", " ")
                .replace('"', '\'').replace(HANGUP, "");
    }

    private static final String[] BLOCKED_KEYWORDS = new String[]{"amara.org", "untertitel"};

    private boolean hasUnderstood(String text) {
        String t = text.toLowerCase();
        for (String blockedKeyword : BLOCKED_KEYWORDS) {
            if (t.contains(blockedKeyword)) {
                return false;
            }
        }

        return true;
    }

    private record AsyncResponse(File responseFile, boolean hangup) {}
}
