package de.asterisk.chatgpt.utils;

public class Constants {

    public static final String OPEN_AI_API_KEY;

    public static final String DEFAULT_MODEL = "gpt-3.5-turbo";

    static {
        OPEN_AI_API_KEY = System.getenv("OPENAI_API_KEY");
    }
}
