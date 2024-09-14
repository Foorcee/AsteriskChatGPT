package de.asterisk.chatgpt.rag;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import de.asterisk.chatgpt.rag.models.SearchResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RAGSearchClient {
    private static final Gson GSON = new Gson();
    private static final String API_URL = System.getenv("RAG_SERVICE_HOST") + "/api/v1/document/search";

    private final HttpClient client = HttpClient.newHttpClient();

    public SearchResult search(String query, int limit, double minRelevance) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format("%s?query=%s&limit=%d&minrelevance=%.2f", API_URL, encodedQuery, limit, minRelevance);

            log.debug("Sending request to {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Type searchResultType = new TypeToken<SearchResult>() {}.getType();
                return GSON.fromJson(response.body(), searchResultType);
            } else {
                throw new RuntimeException("Fehler bei der API-Anfrage: " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex) {
            log.error("Fehler bei der API-Anfrage", ex);
            return null;
        }
    }
}
