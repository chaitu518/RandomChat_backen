package com.srt.randomchat.bot;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BotService {

    private final BotProperties properties;
    private final OllamaClient ollamaClient;
    private final MemoryManager memoryManager;
    private final PromptBuilder promptBuilder;
    private final ConcurrentHashMap<String, AtomicInteger> noQuestionTurns = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong unavailableUntilMs = new java.util.concurrent.atomic.AtomicLong(0);

    public BotService(BotProperties properties,
                      OllamaClient ollamaClient,
                      MemoryManager memoryManager,
                      PromptBuilder promptBuilder) {
        this.properties = properties;
        this.ollamaClient = ollamaClient;
        this.memoryManager = memoryManager;
        this.promptBuilder = promptBuilder;
    }

    public boolean isEnabled() {
        if (!properties.isEnabled()) return false;
        return System.currentTimeMillis() >= unavailableUntilMs.get();
    }

    public void markUnavailable() {
        long delayMs = Math.max(5, properties.getUnavailableSeconds()) * 1000L;
        unavailableUntilMs.set(System.currentTimeMillis() + delayMs);
    }

    public String getBotSenderId() {
        return "anon-bot";
    }

    @Async("botExecutor")
    public CompletableFuture<String> generateReply(String sessionId, String userMessage) {
        if (!properties.isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        boolean forceQuestion = shouldForceQuestion(sessionId, userMessage);
        String prompt = forceQuestion
                ? promptBuilder.buildPromptWithQuestion(sessionId, userMessage)
                : promptBuilder.buildPrompt(sessionId, userMessage);
        String reply = ollamaClient.generate(prompt);

        if (reply == null) {
            markUnavailable();
            return CompletableFuture.completedFuture(null);
        }

        if (shouldRegenerateForSimilarity(sessionId, reply)) {
            String retryPrompt = forceQuestion
                    ? promptBuilder.buildPromptWithQuestion(sessionId, userMessage)
                    : promptBuilder.buildPrompt(sessionId, userMessage);
            String retryReply = ollamaClient.generate(retryPrompt);
            if (retryReply != null && !retryReply.isBlank()) {
                reply = retryReply;
            }
        }

        if (forceQuestion && (reply == null || !reply.contains("?"))) {
            String retryPrompt = promptBuilder.buildPromptWithQuestion(sessionId, userMessage);
            String retryReply = ollamaClient.generate(retryPrompt);
            if (retryReply != null && !retryReply.isBlank()) {
                reply = retryReply;
            }
        }

        if (reply == null || reply.isBlank()) {
            markUnavailable();
            return CompletableFuture.completedFuture(null);
        }

        reply = promptBuilder.trimToShortReply(reply);
        memoryManager.append(sessionId, "user", userMessage);
        memoryManager.append(sessionId, "bot", reply);
        updateQuestionCounter(sessionId, reply);

        applyTypingDelay(reply);
        return CompletableFuture.completedFuture(reply);
    }

    private boolean shouldRegenerateForSimilarity(String sessionId, String reply) {
        String last = getLastBotReply(sessionId);
        if (last == null || last.isBlank() || reply == null || reply.isBlank()) {
            return false;
        }
        return similarity(last, reply) > properties.getSimilarityThreshold();
    }

    private String getLastBotReply(String sessionId) {
        var memory = memoryManager.getRecent(sessionId);
        for (int i = memory.size() - 1; i >= 0; i--) {
            MemoryManager.MemoryEntry entry = memory.get(i);
            if ("bot".equalsIgnoreCase(entry.role())) {
                return entry.content();
            }
        }
        return null;
    }

    private boolean shouldForceQuestion(String sessionId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return true;
        }
        int count = noQuestionTurns.computeIfAbsent(sessionId, id -> new AtomicInteger(0)).get();
        return count >= properties.getMaxNoQuestionTurns();
    }

    private void applyTypingDelay(String reply) {
        int length = reply == null ? 0 : reply.length();
        long delayMs = 800L + (long) length * 15L;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateQuestionCounter(String sessionId, String reply) {
        AtomicInteger counter = noQuestionTurns.computeIfAbsent(sessionId, id -> new AtomicInteger(0));
        if (reply != null && reply.contains("?")) {
            counter.set(0);
        } else {
            counter.incrementAndGet();
        }
    }

    private double similarity(String a, String b) {
        String[] aw = a.toLowerCase().split("\\s+");
        String[] bw = b.toLowerCase().split("\\s+");
        int common = 0;
        for (String w : aw) {
            for (String v : bw) {
                if (w.equals(v)) {
                    common++;
                    break;
                }
            }
        }
        int total = Math.max(1, aw.length + bw.length - common);
        return (double) common / total;
    }
}
