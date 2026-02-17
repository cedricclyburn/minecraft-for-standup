package com.example.minecraftjira;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownService {
    private final Duration cooldown;
    private final Map<UUID, Instant> lastUseByPlayer = new ConcurrentHashMap<>();

    public CooldownService(Duration cooldown) {
        this.cooldown = cooldown;
    }

    public Optional<Duration> tryAcquire(UUID playerId) {
        Instant now = Instant.now();
        Instant previous = lastUseByPlayer.get(playerId);

        if (previous != null) {
            Duration elapsed = Duration.between(previous, now);
            if (elapsed.compareTo(cooldown) < 0) {
                return Optional.of(cooldown.minus(elapsed));
            }
        }

        lastUseByPlayer.put(playerId, now);
        return Optional.empty();
    }
}
