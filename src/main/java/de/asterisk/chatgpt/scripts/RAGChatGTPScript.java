package de.asterisk.chatgpt.scripts;

import de.asterisk.chatgpt.rag.RAGSearchClient;
import de.asterisk.chatgpt.rag.models.Citation;
import de.asterisk.chatgpt.rag.models.Partition;
import de.asterisk.chatgpt.rag.models.SearchResult;
import de.asterisk.chatgpt.resources.PromptResourceManager;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
//TODO: Aktuell bekommen wir nicht die Transkription des Audios daher können wir keine RAG Suche durchführen
public class RAGChatGTPScript extends DefaultChatGPTScript {

    private final RAGSearchClient searchClient = new RAGSearchClient();

    @Override
    public void onHeardText(String text) {
        long startTime = System.currentTimeMillis();
        try {
            SearchResult result = searchClient.search(text, 3, 0.3);
            if (result == null || result.results() == 0) {
                log.info("No relevant text found.");
                return;
            }

            log.info("Found {} relevant text.", result.results());
            addRagCitationSystemMessage(result.citations());
        } finally {
            long timeElapsed = System.currentTimeMillis() - startTime;
            log.info("Search completed in {} ms", timeElapsed);
        }
    }

    private void addRagCitationSystemMessage(List<Citation> citations) {
        String factsPrompt = PromptResourceManager.getPrompt("rag_facts_prompt");

        List<String> facts = new ArrayList<>();
        for (Citation citation : citations) {
            if (facts.size() >= 3)
                break;
            for (Partition partition : citation.partitions()) {
                int id = facts.size() +1;
                String text = partition.text().replace("\n", "");
                facts.add(id + ". " + text
                        + "[Relevance: " + partition.relevance() + "]");
            }
        }

        String factsStr = String.join("\n", facts);
        factsPrompt = factsPrompt.replace("{{facts}}", factsStr);
        log.info("Adding rag citation system message: {}", factsPrompt);

        addSystemMessage(factsPrompt);
    }

    @Override
    protected void addSystemMessage() {
        String systemPrompt = PromptResourceManager.getPrompt("rag_system_prompt");
        addSystemMessage(systemPrompt);
    }
}
