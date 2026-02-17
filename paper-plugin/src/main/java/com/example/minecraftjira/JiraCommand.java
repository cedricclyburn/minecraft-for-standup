package com.example.minecraftjira;

import com.example.minecraftjira.model.BackendResponse;
import com.example.minecraftjira.model.StandupRequest;
import com.example.minecraftjira.model.UpdateRequest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class JiraCommand implements TabExecutor {
    private static final Pattern ITEM_REFERENCE_PATTERN = Pattern.compile("^(PVTI_[A-Za-z0-9]+|[A-Z]+-\\d+|#\\d+|\\d+)$");

    private final JavaPlugin plugin;
    private final BackendClient backendClient;
    private final CooldownService cooldownService;

    public JiraCommand(JavaPlugin plugin, BackendClient backendClient, CooldownService cooldownService) {
        this.plugin = plugin;
        this.backendClient = backendClient;
        this.cooldownService = cooldownService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleHelp(sender);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "help" -> handleHelp(sender);
            case "status" -> handleStatus(sender, args);
            case "update" -> handleUpdate(sender, args);
            case "standup" -> handleStandup(sender, args);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Try /jira help");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("help", "status", "update", "standup"), args[0]);
        }

        if (args.length == 2 && "update".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("PVTI_example", "#123", "ABC-123"), args[1]);
        }

        if (args.length == 3 && "update".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("Backlog", "Todo", "In Progress", "Blocked", "Done"), args[2]);
        }

        return Collections.emptyList();
    }

    private boolean handleHelp(CommandSender sender) {
        if (!hasPermission(sender, "minecraftjira.help")) {
            return true;
        }

        withResponse(sender, backendClient.getHelp(), "Could not load help from backend");
        return true;
    }

    private boolean handleStatus(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "minecraftjira.status")) {
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /jira status <ITEM_ID|ISSUE_KEY>");
            return true;
        }

        if (!allowWithCooldown(sender)) {
            return true;
        }

        String item = join(args, 1);
        withResponse(sender, backendClient.getStatus(item), "Could not fetch status");
        return true;
    }

    private boolean handleUpdate(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "minecraftjira.update")) {
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /jira update <ITEM_ID> <NEW_STATUS>");
            sender.sendMessage(ChatColor.YELLOW + "   or: /jira update <natural language>");
            return true;
        }

        if (!allowWithCooldown(sender)) {
            return true;
        }

        String actor = sender.getName();
        UpdateRequest request;

        if (args.length >= 3 && isItemReference(args[1])) {
            request = new UpdateRequest(args[1], join(args, 2), null, actor);
        } else {
            request = new UpdateRequest(null, null, join(args, 1), actor);
        }

        withResponse(sender, backendClient.update(request), "Could not update status");
        return true;
    }

    private boolean handleStandup(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "minecraftjira.standup")) {
            return true;
        }

        if (!allowWithCooldown(sender)) {
            return true;
        }

        String prompt = args.length > 1 ? join(args, 1) : null;
        StandupRequest request = new StandupRequest(prompt, sender.getName());
        withResponse(sender, backendClient.standup(request), "Could not build standup summary");
        return true;
    }

    private void withResponse(CommandSender sender, CompletableFuture<BackendResponse> future, String fallbackError) {
        future.whenComplete((response, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) {
                plugin.getLogger().warning("Backend call failed: " + error.getMessage());
                sender.sendMessage(ChatColor.RED + fallbackError + ": " + error.getMessage());
                return;
            }

            if (response == null) {
                sender.sendMessage(ChatColor.RED + fallbackError + ": empty response");
                return;
            }

            if (!response.isOk()) {
                sender.sendMessage(ChatColor.RED + safe(response.getMessage(), fallbackError));
                return;
            }

            String message = buildChatMessage(response);
            sender.sendMessage(ChatColor.GREEN + message);
        }));
    }

    private String buildChatMessage(BackendResponse response) {
        String message = safe(response.getMessage(), "Request completed");
        JsonObject data = response.getData();
        if (data == null || data.size() == 0) {
            return message;
        }

        List<String> details = new ArrayList<>();
        appendIfPresent(details, data, "item");
        appendIfPresent(details, data, "status");
        appendIfPresent(details, data, "newStatus");
        appendIfPresent(details, data, "summary");

        if (details.isEmpty()) {
            return message;
        }

        return message + " | " + String.join(" | ", details);
    }

    private void appendIfPresent(List<String> details, JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        if (value == null || value.isJsonNull()) {
            return;
        }

        if (value.isJsonPrimitive()) {
            details.add(key + "=" + value.getAsString());
        }
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }

        sender.sendMessage(ChatColor.RED + "You do not have permission: " + permission);
        return false;
    }

    private boolean allowWithCooldown(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        UUID playerId = player.getUniqueId();
        Optional<Duration> remaining = cooldownService.tryAcquire(playerId);
        if (remaining.isEmpty()) {
            return true;
        }

        long seconds = Math.max(1, remaining.get().toSeconds());
        sender.sendMessage(ChatColor.YELLOW + "Cooldown active. Try again in " + seconds + "s.");
        return false;
    }

    private List<String> filter(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                result.add(option);
            }
        }
        return result;
    }

    private String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private boolean isItemReference(String value) {
        return ITEM_REFERENCE_PATTERN.matcher(value).matches();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
