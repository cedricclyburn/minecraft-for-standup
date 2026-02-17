package com.example.minecraftjira.backend.service;

import com.example.minecraftjira.backend.model.StandupData;
import com.example.minecraftjira.backend.model.UpdateIntent;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class LlmIntentService {
    private static final Pattern ITEM_PATTERN = Pattern.compile("(PVTI_[A-Za-z0-9]+|[A-Z]+-\\d+|#\\d+)");

    private static final Map<String, String> STATUS_ALIASES = buildStatusAliases();

    @ConfigProperty(name = "llm.enabled", defaultValue = "false")
    boolean llmEnabled;

    @ConfigProperty(name = "llm.base-url", defaultValue = "")
    String llmBaseUrl;

    @ConfigProperty(name = "llm.model", defaultValue = "local-model")
    String llmModel;

    public UpdateIntent parseUpdateIntent(String text) {
        String item = extractItem(text);
        String status = extractStatus(text);
        return new UpdateIntent("update_status", item, status, text);
    }

    public Optional<String> normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }

        String normalized = STATUS_ALIASES.get(status.toLowerCase(Locale.ROOT).trim());
        if (normalized == null) {
            return Optional.of(status.trim());
        }

        return Optional.of(normalized);
    }

    public StandupData generateStandupSummary(String prompt) {
        String summary;
        String source;
        List<String> blockers = new ArrayList<>();

        if (llmEnabled && llmBaseUrl != null && !llmBaseUrl.isBlank()) {
            summary = "LLM endpoint configured at " + llmBaseUrl + " (model=" + llmModel + "). Stub mode active.";
            source = "llm-stub";
        } else {
            summary = "POC summary: 2 items in progress, 1 blocked on tokenizer bug, 1 ready for review.";
            source = "local-stub";
        }

        if (prompt != null && !prompt.isBlank()) {
            blockers.add("Prompt received: " + prompt);
        }
        blockers.add("Tokenizer bug unresolved");

        return new StandupData(summary, blockers, source);
    }

    private String extractItem(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = ITEM_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : STATUS_ALIASES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static Map<String, String> buildStatusAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("backlog", "Backlog");
        aliases.put("todo", "Todo");
        aliases.put("to do", "Todo");
        aliases.put("in progress", "In Progress");
        aliases.put("doing", "In Progress");
        aliases.put("blocked", "Blocked");
        aliases.put("on hold", "Blocked");
        aliases.put("done", "Done");
        aliases.put("complete", "Done");
        aliases.put("completed", "Done");
        return aliases;
    }
}
