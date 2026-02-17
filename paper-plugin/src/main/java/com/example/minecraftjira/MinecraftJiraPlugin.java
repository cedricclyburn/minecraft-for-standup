package com.example.minecraftjira;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public class MinecraftJiraPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        String backendUrl = getConfig().getString("backend.base-url", "http://localhost:8080");
        String apiKey = getConfig().getString("backend.api-key", "change-me");
        int connectTimeoutMs = getConfig().getInt("backend.connect-timeout-ms", 3000);
        int requestTimeoutMs = getConfig().getInt("backend.request-timeout-ms", 5000);
        int cooldownSeconds = getConfig().getInt("command.cooldown-seconds", 3);

        if ("change-me".equals(apiKey)) {
            getLogger().warning("backend.api-key is still the default value. Update plugins/MinecraftJira/config.yml");
        }

        BackendClient backendClient = new BackendClient(this, backendUrl, apiKey, connectTimeoutMs, requestTimeoutMs);
        CooldownService cooldownService = new CooldownService(Duration.ofSeconds(cooldownSeconds));

        PluginCommand jira = getCommand("jira");
        if (jira == null) {
            throw new IllegalStateException("/jira command is missing from plugin.yml");
        }

        JiraCommand handler = new JiraCommand(this, backendClient, cooldownService);
        jira.setExecutor(handler);
        jira.setTabCompleter(handler);

        getLogger().info("MinecraftJira plugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("MinecraftJira plugin disabled");
    }
}
