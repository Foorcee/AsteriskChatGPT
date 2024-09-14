package de.asterisk.chatgpt.rag.models;

import java.util.List;

public record SearchResult(String query, int results, List<Citation> citations) {
}
