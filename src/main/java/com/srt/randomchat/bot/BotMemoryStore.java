package com.srt.randomchat.bot;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class BotMemoryStore {

    public record MemoryEntry(String role, String content, Instant at) {
    }

    private final BotProperties properties;
    private final Map<String, Deque<MemoryEntry>> memoryBySession = new ConcurrentHashMap<>();

    public BotMemoryStore(BotProperties properties) {
        this.properties = properties;
    }

    public void append(String sessionId, String role, String content) {
        if (sessionId == null || role == null || content == null) {
            return;
        }
        Deque<MemoryEntry> deque = memoryBySession.computeIfAbsent(sessionId, id -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            deque.addLast(new MemoryEntry(role, content, Instant.now()));
            int limit = Math.max(10, Math.min(15, properties.getMemoryLimit()));
            while (deque.size() > limit) {
                deque.pollFirst();
            }
        }
    }

    public List<MemoryEntry> getRecent(String sessionId) {
        Deque<MemoryEntry> deque = memoryBySession.get(sessionId);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return new ArrayList<>(deque);
        }
    }

    public void clear(String sessionId) {
        Deque<MemoryEntry> deque = memoryBySession.remove(sessionId);
        if (deque != null) {
            synchronized (deque) {
                deque.clear();
            }
        }
    }
}
