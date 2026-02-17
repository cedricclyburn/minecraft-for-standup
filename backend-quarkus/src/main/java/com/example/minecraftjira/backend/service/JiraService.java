package com.example.minecraftjira.backend.service;

import com.example.minecraftjira.backend.model.HelpData;
import com.example.minecraftjira.backend.model.StandupData;
import com.example.minecraftjira.backend.model.StandupRequest;
import com.example.minecraftjira.backend.model.StatusData;
import com.example.minecraftjira.backend.model.UpdateData;
import com.example.minecraftjira.backend.model.UpdateIntent;
import com.example.minecraftjira.backend.model.UpdateRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JiraService {
    @Inject
    GitHubProjectsService gitHubProjectsService;

    @Inject
    LlmIntentService llmIntentService;

    public HelpData help() {
        return new HelpData(
                List.of(
                        "/jira help",
                        "/jira status <ITEM_ID|ISSUE_KEY>",
                        "/jira update <ITEM_ID> <NEW_STATUS>",
                        "/jira update <natural language>",
                        "/jira standup [optional prompt]"
                ),
                "Natural language requests are parsed in backend and validated before updates"
        );
    }

    public StatusData status(String item) {
        return gitHubProjectsService.fetchStatus(item);
    }

    public UpdateData update(UpdateRequest request) {
        ResolvedUpdate resolvedUpdate = resolveUpdate(request);
        return gitHubProjectsService.updateStatus(resolvedUpdate.item(), resolvedUpdate.status());
    }

    public StandupData standup(StandupRequest request) {
        String prompt = request == null ? null : request.prompt();
        return llmIntentService.generateStandupSummary(prompt);
    }

    private ResolvedUpdate resolveUpdate(UpdateRequest request) {
        if (request.text() != null && !request.text().isBlank()) {
            UpdateIntent intent = llmIntentService.parseUpdateIntent(request.text());
            String itemFromIntent = intent.item();
            String statusFromIntent = intent.status();
            if (itemFromIntent == null || itemFromIntent.isBlank()) {
                throw new BadRequestException("Could not resolve item from natural-language input");
            }
            if (statusFromIntent == null || statusFromIntent.isBlank()) {
                throw new BadRequestException("Could not resolve target status from natural-language input");
            }

            String normalizedStatus = llmIntentService.normalizeStatus(statusFromIntent).orElse(statusFromIntent);
            return new ResolvedUpdate(itemFromIntent, normalizedStatus);
        }

        if (request.item() == null || request.item().isBlank()) {
            throw new BadRequestException("item is required when text is not provided");
        }
        if (request.newStatus() == null || request.newStatus().isBlank()) {
            throw new BadRequestException("newStatus is required when text is not provided");
        }

        String normalizedStatus = llmIntentService.normalizeStatus(request.newStatus()).orElse(request.newStatus());
        return new ResolvedUpdate(request.item(), normalizedStatus);
    }

    private record ResolvedUpdate(String item, String status) {
    }
}
