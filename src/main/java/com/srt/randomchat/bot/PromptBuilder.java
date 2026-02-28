package com.srt.randomchat.bot;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;

@Component
public class PromptBuilder {

    private final BotProperties properties;
    private final BotIdentityManager identityManager;
    private final MemoryManager memoryManager;

    public PromptBuilder(BotProperties properties, BotIdentityManager identityManager, MemoryManager memoryManager) {
        this.properties = properties;
        this.identityManager = identityManager;
        this.memoryManager = memoryManager;
    }

    public String buildPrompt(String sessionId, String userMessage) {
        return buildPromptInternal(sessionId, userMessage, false);
    }

    public String buildPromptWithQuestion(String sessionId, String userMessage) {
        return buildPromptInternal(sessionId, userMessage, true);
    }

    public String trimToShortReply(String reply) {
        if (reply == null || reply.isBlank()) {
            return "Sorry, can you say that again?";
        }
        String normalized = reply.replace('\n', ' ').trim();
        String[] words = normalized.split("\\s+");
        int maxWords = Math.max(1, properties.getMaxWords());
        if (words.length <= maxWords) {
            return normalized;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) builder.append(' ');
            builder.append(words[i]);
        }
        return builder.toString();
    }

    private String buildPromptInternal(String sessionId, String userMessage, boolean forceQuestion) {
        BotIdentityManager.BotIdentity identity = identityManager.getOrCreate(sessionId);
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("You are " + identity.name() + ", age " + identity.age() + ", from " + identity.city() + ".");
        joiner.add("Never contradict these facts.");
        joiner.add("Interests: " + String.join(", ", identity.interests()) + ".");
        joiner.add("Tone: " + identity.tone() + ".");
        joiner.add("Reply in 3-4 words only.");
        if (forceQuestion) {
            joiner.add("Make it a short question.");
        }
        joiner.add("Conversation so far:");

        List<MemoryManager.MemoryEntry> memory = memoryManager.getRecent(sessionId);
        for (MemoryManager.MemoryEntry entry : memory) {
            joiner.add(formatRole(entry.role(), identity) + ": " + entry.content());
        }

        joiner.add("User: " + userMessage);
        joiner.add(identity.name() + ":");
        return joiner.toString();
    }

    private String formatRole(String role, BotIdentityManager.BotIdentity identity) {
        if ("bot".equalsIgnoreCase(role)) {
            return identity.name();
        }
        return "User";
    }
}
