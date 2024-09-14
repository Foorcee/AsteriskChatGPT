package de.asterisk.chatgpt.rag.models;

import java.util.List;
import java.util.UUID;

public record Citation(UUID documentId, String fileName, List<Partition> partitions) {
}
