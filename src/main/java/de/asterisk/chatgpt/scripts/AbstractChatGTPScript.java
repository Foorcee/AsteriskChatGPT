package de.asterisk.chatgpt.scripts;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.*;
import de.asterisk.chatgpt.resources.AsteriskSoundManager;
import de.asterisk.chatgpt.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.asteriskjava.fastagi.BaseAgiScript;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class AbstractChatGTPScript extends BaseAgiScript {

    private static final String HANGUP_FUNCTION = "hangup";
    protected static final String AUDIO_FORMAT = "wav";

    private final List<ChatCompletionMessageParam> messages = new ArrayList<>();
    private final OpenAIClient openAiService;

    public AbstractChatGTPScript() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null)
            throw new IllegalStateException("OpenAI API key is not set");

        this.openAiService = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    }

    public void addSystemMessage(String text) {
        messages.add(ChatCompletionMessageParam.ofSystem(ChatCompletionSystemMessageParam.builder()
                .content(text).build()));
    }

    public void addAssistantMessage(String text) {
        messages.add(ChatCompletionMessageParam.ofAssistant(ChatCompletionAssistantMessageParam.builder()
                .content(text).build()));
    }

    public GenerationResult generateAudioResponseResponse(AsteriskSoundManager.SessionGuard sessionGuard) throws IOException {
        long startTime = System.currentTimeMillis();
        var request = ChatCompletionCreateParams.builder().model(ChatModel.GPT_4O_MINI_AUDIO_PREVIEW)
                .modalities(List.of(ChatCompletionModality.AUDIO, ChatCompletionModality.TEXT))
                .addTool(ChatCompletionTool.builder()
                        .function(FunctionDefinition.builder()
                                .name(HANGUP_FUNCTION)
                                .description("Beendet das aktuelle Telefonat.")
                                .build())
                        .build())
                .audio(ChatCompletionAudioParam.builder()
                        .format(ChatCompletionAudioParam.Format.WAV)
                        .voice(ChatCompletionAudioParam.Voice.ALLOY)
                        .build())
                        .messages(messages).build();

        var response = openAiService.chat().completions().create(request);
        var result = new GenerationResult();

        var message = response.choices().getFirst().message();
        log.info("Received response in {}ms", System.currentTimeMillis() - startTime);
        message.audio().ifPresent(audio -> {
            long startTimeAudio = System.currentTimeMillis();
            var file = sessionGuard.getAudioFile("response.wav");
            var data = audio.data();
            try {
                FileUtils.writeBase64ToFile(data, file);
                FileUtils.convertAudioFile(file);

                result.setAudioFile(file);
                messages.add(ChatCompletionMessageParam.ofAssistant(ChatCompletionAssistantMessageParam.builder()
                        .audio(ChatCompletionAssistantMessageParam.Audio
                                .builder().id(audio.id()).build())
                        .build()));
                log.info("Received content {}", audio.transcript());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                log.info("Audio file converted in {}ms", System.currentTimeMillis() - startTimeAudio);
            }
        });

        //TODO: Die Response ist aktuell nur entweder Function-Call oder Message
        // Dadurch ist die Konversation aktuell etwas komisch
        message.toolCalls().ifPresent(calls -> {
            boolean isHangup = calls.stream()
                    .anyMatch(call -> call.function().name().equalsIgnoreCase(HANGUP_FUNCTION));
            result.setHangup(isHangup);
        });

        log.info("Result created in {}ms", System.currentTimeMillis() - startTime);
        return result;
    }

    public void addUserAudioRecording(File file) throws IOException {
        var base64data = FileUtils.readFileAsBase64(file);

        var audioParam = ChatCompletionContentPartInputAudio.builder()
                .inputAudio(ChatCompletionContentPartInputAudio.InputAudio.builder()
                        .format(ChatCompletionContentPartInputAudio.InputAudio.Format.WAV)
                        .data(base64data)
                        .build()).build();
        var audioContent = ChatCompletionContentPart.ofInputAudio(audioParam);

        messages.add(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder()
                .contentOfArrayOfContentParts(Collections.singletonList(audioContent)).build()));
    }
}
