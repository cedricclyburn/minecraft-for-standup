package com.example.minecraftjira.backend.service;

import com.example.minecraftjira.backend.github.GitHubGraphqlClient;
import com.example.minecraftjira.backend.model.ProjectContext;
import com.example.minecraftjira.backend.model.StatusData;
import com.example.minecraftjira.backend.model.UpdateData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class GitHubProjectsService {
    private static final Logger LOG = Logger.getLogger(GitHubProjectsService.class);

    @Inject
    GitHubGraphqlClient gitHubGraphqlClient;

    @ConfigProperty(name = "github.token", defaultValue = "")
    String githubToken;

    @ConfigProperty(name = "github.org")
    String githubOrg;

    @ConfigProperty(name = "github.project-number")
    int githubProjectNumber;

    @ConfigProperty(name = "github.repository")
    String githubRepository;

    public StatusData fetchStatus(String itemRef) {
        if (!isConfigured()) {
            return new StatusData(itemRef, "UNKNOWN", "stub-no-github-token");
        }

        String itemId = resolveItemId(itemRef);
        String query = """
                query($itemId: ID!) {
                  node(id: $itemId) {
                    ... on ProjectV2Item {
                      id
                      fieldValueByName(name: \"Status\") {
                        ... on ProjectV2ItemFieldSingleSelectValue {
                          name
                        }
                      }
                    }
                  }
                }
                """;

        JsonObject variables = new JsonObject();
        variables.addProperty("itemId", itemId);

        JsonObject root = gitHubGraphqlClient.execute(query, variables);
        JsonObject node = root.getAsJsonObject("data").getAsJsonObject("node");

        if (node == null || node.isJsonNull()) {
            return new StatusData(itemRef, "UNKNOWN", "github-project-v2");
        }

        JsonObject fieldValue = node.getAsJsonObject("fieldValueByName");
        String status = fieldValue == null || fieldValue.isJsonNull() ? "UNKNOWN" : fieldValue.get("name").getAsString();
        return new StatusData(itemRef, status, "github-project-v2");
    }

    public UpdateData updateStatus(String itemRef, String newStatus) {
        if (!isConfigured()) {
            return new UpdateData(itemRef, "UNKNOWN", newStatus, "stub-no-github-token");
        }

        String itemId = resolveItemId(itemRef);
        StatusData previous = fetchStatus(itemId);

        ProjectContext context = loadProjectContext();
        String normalized = newStatus.trim().toLowerCase(Locale.ROOT);
        String optionId = context.statusOptionIds().get(normalized);
        if (optionId == null) {
            throw new IllegalArgumentException("Status option not found in project: " + newStatus);
        }

        String mutation = """
                mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
                  updateProjectV2ItemFieldValue(
                    input: {
                      projectId: $projectId
                      itemId: $itemId
                      fieldId: $fieldId
                      value: { singleSelectOptionId: $optionId }
                    }
                  ) {
                    projectV2Item {
                      id
                    }
                  }
                }
                """;

        JsonObject variables = new JsonObject();
        variables.addProperty("projectId", context.projectId());
        variables.addProperty("itemId", itemId);
        variables.addProperty("fieldId", context.statusFieldId());
        variables.addProperty("optionId", optionId);

        gitHubGraphqlClient.execute(mutation, variables);
        return new UpdateData(itemRef, previous.status(), newStatus, "github-project-v2");
    }

    private ProjectContext loadProjectContext() {
        String query = """
                query($org: String!, $number: Int!) {
                  organization(login: $org) {
                    projectV2(number: $number) {
                      id
                      fields(first: 50) {
                        nodes {
                          ... on ProjectV2SingleSelectField {
                            id
                            name
                            options {
                              id
                              name
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        JsonObject variables = new JsonObject();
        variables.addProperty("org", githubOrg);
        variables.addProperty("number", githubProjectNumber);

        JsonObject root = gitHubGraphqlClient.execute(query, variables);
        JsonObject project = root.getAsJsonObject("data")
                .getAsJsonObject("organization")
                .getAsJsonObject("projectV2");

        if (project == null || project.isJsonNull()) {
            throw new IllegalStateException("Project not found: org=" + githubOrg + " number=" + githubProjectNumber);
        }

        String projectId = project.get("id").getAsString();
        JsonArray fields = project.getAsJsonObject("fields").getAsJsonArray("nodes");

        String statusFieldId = null;
        Map<String, String> optionsByNormalizedName = new HashMap<>();

        for (JsonElement fieldElement : fields) {
            if (!fieldElement.isJsonObject()) {
                continue;
            }
            JsonObject field = fieldElement.getAsJsonObject();
            JsonElement nameElement = field.get("name");
            if (nameElement == null || nameElement.isJsonNull()) {
                continue;
            }

            String fieldName = nameElement.getAsString();
            if (!"Status".equalsIgnoreCase(fieldName)) {
                continue;
            }

            statusFieldId = field.get("id").getAsString();
            JsonArray options = field.getAsJsonArray("options");
            for (JsonElement optionElement : options) {
                JsonObject option = optionElement.getAsJsonObject();
                String optionId = option.get("id").getAsString();
                String optionName = option.get("name").getAsString();
                optionsByNormalizedName.put(optionName.toLowerCase(Locale.ROOT), optionId);
            }
            break;
        }

        if (statusFieldId == null) {
            throw new IllegalStateException("Status field not found in project");
        }

        return new ProjectContext(projectId, statusFieldId, optionsByNormalizedName);
    }

    private String resolveItemId(String itemRef) {
        if (itemRef.startsWith("PVTI_")) {
            return itemRef;
        }

        if (itemRef.startsWith("#")) {
            Integer issueNumber = tryParseIssueNumber(itemRef.substring(1));
            if (issueNumber != null) {
                String itemId = findItemIdByIssueNumber(issueNumber);
                if (itemId != null) {
                    return itemId;
                }
            }
        }

        throw new IllegalArgumentException(
                "Unsupported item reference: " + itemRef + ". Use PVTI_* item id or #<issueNumber> present in configured project"
        );
    }

    private String findItemIdByIssueNumber(int issueNumber) {
        String[] parts = githubRepository.split("/");
        if (parts.length != 2) {
            LOG.warnf("github.repository must be owner/repo. Current value: %s", githubRepository);
            return null;
        }

        String owner = parts[0];
        String repository = parts[1];

        String query = """
                query($org: String!, $number: Int!) {
                  organization(login: $org) {
                    projectV2(number: $number) {
                      items(first: 100) {
                        nodes {
                          id
                          content {
                            ... on Issue {
                              number
                              repository {
                                owner {
                                  login
                                }
                                name
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        JsonObject variables = new JsonObject();
        variables.addProperty("org", githubOrg);
        variables.addProperty("number", githubProjectNumber);

        JsonObject root = gitHubGraphqlClient.execute(query, variables);
        JsonArray items = root.getAsJsonObject("data")
                .getAsJsonObject("organization")
                .getAsJsonObject("projectV2")
                .getAsJsonObject("items")
                .getAsJsonArray("nodes");

        for (JsonElement itemElement : items) {
            if (!itemElement.isJsonObject()) {
                continue;
            }
            JsonObject item = itemElement.getAsJsonObject();
            JsonObject content = item.getAsJsonObject("content");
            if (content == null || content.isJsonNull() || !content.has("number")) {
                continue;
            }

            int number = content.get("number").getAsInt();
            JsonObject repo = content.getAsJsonObject("repository");
            String repoOwner = repo.getAsJsonObject("owner").get("login").getAsString();
            String repoName = repo.get("name").getAsString();

            if (number == issueNumber && owner.equals(repoOwner) && repository.equals(repoName)) {
                return item.get("id").getAsString();
            }
        }

        return null;
    }

    private Integer tryParseIssueNumber(String maybeNumber) {
        try {
            return Integer.parseInt(maybeNumber);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isConfigured() {
        if (githubToken == null || githubToken.isBlank()) {
            return false;
        }

        String normalized = githubToken.trim().toLowerCase(Locale.ROOT);
        return !normalized.startsWith("placeholder") && !normalized.startsWith("change_me");
    }
}
