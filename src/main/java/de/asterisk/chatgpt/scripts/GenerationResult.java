package de.asterisk.chatgpt.scripts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.util.Optional;

@ToString
@Setter
public class GenerationResult {
    private File audioFile;
    @Getter
    private boolean hangup;

    public Optional<File> getAudioFile() {
        return Optional.ofNullable(audioFile);
    }
}
