package com.srt.randomchat.bot;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class BotIdentityManager {

    public record BotIdentity(String name, int age, String city, List<String> interests, String tone) {
    }

    private final Map<String, BotIdentity> identityBySession = new ConcurrentHashMap<>();
    private final List<BotIdentity> templates = List.of(
            new BotIdentity("Meera", 22, "Bangalore", List.of("music", "travel"), "friendly, casual"),
            new BotIdentity("Anaya", 24, "Delhi", List.of("books", "coffee"), "warm, curious"),
            new BotIdentity("Ira", 21, "Pune", List.of("movies", "food"), "light, playful"),
            new BotIdentity("Riya", 26, "Chennai", List.of("art", "fitness"), "calm, kind"),
            new BotIdentity("Nisha", 23, "Mumbai", List.of("nature", "photography"), "soft, friendly")
    );

    public BotIdentity getOrCreate(String sessionId) {
        return identityBySession.computeIfAbsent(sessionId, id -> templates.get(randomIndex()));
    }

    public void clear(String sessionId) {
        identityBySession.remove(sessionId);
    }

    private int randomIndex() {
        return ThreadLocalRandom.current().nextInt(templates.size());
    }
}
