package de.asterisk.chatgpt.scripts;

import de.asterisk.chatgpt.resources.AsteriskSoundManager;
import de.asterisk.chatgpt.resources.PromptResourceManager;
import lombok.extern.slf4j.Slf4j;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;

import java.io.File;
import java.io.IOException;

@Slf4j
public class DefaultChatGPTScript extends AbstractChatGTPScript {

    private final String welcomeMessage;

    public DefaultChatGPTScript() {
        addSystemMessage();

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
                addUserAudioRecording(recording);

                //Get response from assistant
                var result = generateAudioResponseResponse(session);

                result.getAudioFile().ifPresent(responseFile -> {
                    log.info("Playing response...");
                    try {
                        playAudioFile(responseFile);
                    } catch (AgiException e) {
                        throw new RuntimeException(e);
                    }
                });

                if (result.isHangup()) {
                    log.info("Call ended by assistant");
                    hangup();
                    return;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void addSystemMessage() {
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
        //TODO: getTextToSpeech(text, audioFile);
        return audioFile;
    }

    private File getWelcomeAudioFile() {
        return getCachedTextToSpeech("welcome", welcomeMessage);
    }

    private void playAudioFile(File file) throws AgiException {
        String fileName = file.getAbsolutePath().replace(".wav", "");
        streamFile(fileName);
    }
}
