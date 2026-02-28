package com.srt.randomchat.bot;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationController {

    public record ConversationPlan(String mood, boolean forceQuestion, boolean addCuriosityHook) {
    }

    private final Map<String, ConversationState> stateBySession = new ConcurrentHashMap<>();

    public ConversationPlan planFor(String sessionId, String userMessage, int noQuestionTurns) {
        ConversationState state = stateBySession.computeIfAbsent(sessionId, id -> new ConversationState());
        state.incrementMessageCount();

        String mood = state.resolveMood();
        boolean shortReply = isShort(userMessage);
        boolean forceQuestion = shortReply || noQuestionTurns >= 2;
        boolean addCuriosityHook = state.getMessageCount() > 6;

        return new ConversationPlan(mood, forceQuestion, addCuriosityHook);
    }

    public void onBotReply(String sessionId, String reply) {
        ConversationState state = stateBySession.computeIfAbsent(sessionId, id -> new ConversationState());
        state.setLastBotReply(reply);
        if (reply != null && reply.contains("?")) {
            state.incrementQuestionCount();
        }
    }

    public String getLastBotReply(String sessionId) {
        ConversationState state = stateBySession.get(sessionId);
        return state == null ? null : state.getLastBotReply();
    }

    private boolean isShort(String message) {
        if (message == null || message.isBlank()) {
            return true;
        }
        String[] words = message.trim().split("\\s+");
        return words.length <= 3;
    }

    private static class ConversationState {
        private int messageCount;
        private int questionCount;
        private String lastBotReply;

        synchronized void incrementMessageCount() {
            messageCount++;
        }

        synchronized void incrementQuestionCount() {
            questionCount++;
        }

        synchronized int getMessageCount() {
            return messageCount;
        }

        synchronized String getLastBotReply() {
            return lastBotReply;
        }

        synchronized void setLastBotReply(String reply) {
            this.lastBotReply = reply;
        }

        synchronized String resolveMood() {
            if (messageCount <= 3) return "casual";
            if (messageCount <= 5) return "playful";
            if (messageCount <= 8) return "personal";
            return "deep";
        }
    }
}
