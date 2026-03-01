package com.srt.randomchat.service;

import com.srt.randomchat.model.Gender;
import com.srt.randomchat.model.MatchOutcome;
import com.srt.randomchat.model.MatchResult;
import com.srt.randomchat.model.MatchStats;
import com.srt.randomchat.model.Preference;
import com.srt.randomchat.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;

@Service
public class MatchService {

    private static final String BOT_SESSION_ID = "BOT";

    private record Room(String sessionA, String sessionB) {
        String other(String sessionId) {
            if (sessionA.equals(sessionId)) return sessionB;
            if (sessionB.equals(sessionId)) return sessionA;
            return null;
        }
    }

    private final Map<String, UserProfile> profiles    = new ConcurrentHashMap<>();
    private final Queue<String>            waiting     = new ConcurrentLinkedQueue<>();
    private final Map<String, String>      roomBySession = new ConcurrentHashMap<>();
    private final Map<String, Room>        rooms       = new ConcurrentHashMap<>();
    private final Map<String, String>      botRoomBySession = new ConcurrentHashMap<>();

    public synchronized String register(String sessionId, Gender gender, Preference preference) {
        UserProfile existing = profiles.get(sessionId);
        if (existing != null) {
            profiles.put(sessionId, new UserProfile(gender, preference, existing.anonymousId()));
            return existing.anonymousId();
        }
        String anonymousId = "anon-" + UUID.randomUUID().toString().substring(0, 8);
        profiles.put(sessionId, new UserProfile(gender, preference, anonymousId));
        return anonymousId;
    }

    public synchronized Optional<MatchOutcome> requestMatch(String sessionId) {
        UserProfile profile = profiles.get(sessionId);
        if (profile == null) return Optional.empty();
        if (roomBySession.containsKey(sessionId)) return Optional.empty();

        waiting.remove(sessionId);

        Optional<MatchResult> strictMatch = tryMatch(sessionId, profile, this::isCompatible);
        if (strictMatch.isPresent()) return Optional.of(new MatchOutcome(strictMatch.get(), null, null));

        if (shouldFallbackToAnyGender(profile)) {
            Optional<MatchResult> fallbackMatch = tryMatch(sessionId, profile, this::isFallbackCompatible);
            if (fallbackMatch.isPresent()) return Optional.of(new MatchOutcome(fallbackMatch.get(), null, null));
        }

        Optional<MatchOutcome> botRoomMatch = tryMatchFromBotRooms(sessionId, profile, this::isCompatible);
        if (botRoomMatch.isPresent()) return botRoomMatch;

        if (shouldFallbackToAnyGender(profile)) {
            Optional<MatchOutcome> fallbackBotMatch = tryMatchFromBotRooms(sessionId, profile, this::isFallbackCompatible);
            if (fallbackBotMatch.isPresent()) return fallbackBotMatch;
        }

        if (!waiting.contains(sessionId)) waiting.add(sessionId);
        return Optional.empty();
    }

    public synchronized Optional<String> assignBotRoom(String sessionId) {
        UserProfile profile = profiles.get(sessionId);
        if (profile == null) return Optional.empty();
        if (roomBySession.containsKey(sessionId)) return Optional.empty();

        waiting.remove(sessionId);
        String roomId = "bot-" + UUID.randomUUID();
        rooms.put(roomId, new Room(sessionId, BOT_SESSION_ID));
        roomBySession.put(sessionId, roomId);
        botRoomBySession.put(sessionId, roomId);
        return Optional.of(roomId);
    }

    public synchronized Optional<String> getRoom(String sessionId) {
        return Optional.ofNullable(roomBySession.get(sessionId));
    }

    public synchronized boolean isBotRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        return BOT_SESSION_ID.equals(room.sessionA()) || BOT_SESSION_ID.equals(room.sessionB());
    }

    public synchronized Optional<String> leaveRoom(String sessionId) {
        String roomId = roomBySession.remove(sessionId);
        if (roomId == null) return Optional.empty();
        Room room = rooms.remove(roomId);
        if (room == null) return Optional.empty();
        botRoomBySession.remove(sessionId);
        String partnerId = room.other(sessionId);
        if (partnerId != null && !BOT_SESSION_ID.equals(partnerId)) {
            roomBySession.remove(partnerId);
            return Optional.of(partnerId);
        }
        return Optional.empty();
    }

    public synchronized Optional<String> handleDisconnect(String sessionId) {
        waiting.remove(sessionId);
        Optional<String> partnerId = leaveRoom(sessionId);
        profiles.remove(sessionId);
        botRoomBySession.remove(sessionId);
        return partnerId;
    }

    public synchronized void cancelSearch(String sessionId) {
        waiting.remove(sessionId);
    }

    public synchronized Optional<String> getAnonymousId(String sessionId) {
        UserProfile profile = profiles.get(sessionId);
        return profile == null ? Optional.empty() : Optional.of(profile.anonymousId());
    }

    public synchronized boolean isRegistered(String sessionId) {
        return profiles.containsKey(sessionId);
    }

    public synchronized MatchStats getStats() {
        return new MatchStats(profiles.size(), waiting.size(), rooms.size());
    }

    public synchronized boolean isInRoom(String roomId, String sessionId) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        return sessionId.equals(room.sessionA()) || sessionId.equals(room.sessionB());
    }

    private Optional<MatchResult> tryMatch(String sessionId,
                                           UserProfile profile,
                                           BiPredicate<UserProfile, UserProfile> compatibility) {
        for (String otherId : waiting) {
            if (sessionId.equals(otherId)) continue;
            UserProfile other = profiles.get(otherId);
            if (other == null) { waiting.remove(otherId); continue; }
            if (compatibility.test(profile, other)) {
                waiting.remove(otherId);
                String roomId = UUID.randomUUID().toString();
                roomBySession.put(sessionId, roomId);
                roomBySession.put(otherId, roomId);
                rooms.put(roomId, new Room(sessionId, otherId));
                return Optional.of(new MatchResult(roomId, sessionId, otherId));
            }
        }
        return Optional.empty();
    }

    private Optional<MatchOutcome> tryMatchFromBotRooms(String sessionId,
                                                        UserProfile profile,
                                                        BiPredicate<UserProfile, UserProfile> compatibility) {
        for (Map.Entry<String, String> entry : botRoomBySession.entrySet()) {
            String otherId = entry.getKey();
            if (sessionId.equals(otherId)) continue;
            UserProfile other = profiles.get(otherId);
            if (other == null) {
                botRoomBySession.remove(otherId);
                continue;
            }
            if (compatibility.test(profile, other)) {
                String oldRoomId = entry.getValue();
                rooms.remove(oldRoomId);
                roomBySession.remove(otherId);
                botRoomBySession.remove(otherId);

                String roomId = UUID.randomUUID().toString();
                roomBySession.put(sessionId, roomId);
                roomBySession.put(otherId, roomId);
                rooms.put(roomId, new Room(sessionId, otherId));
                MatchResult result = new MatchResult(roomId, sessionId, otherId);
                return Optional.of(new MatchOutcome(result, otherId, oldRoomId));
            }
        }
        return Optional.empty();
    }

    private boolean shouldFallbackToAnyGender(UserProfile profile) {
        return profile.preference() != Preference.BOTH;
    }

    private boolean isFallbackCompatible(UserProfile requester, UserProfile other) {
        return true;
    }

    private boolean isCompatible(UserProfile a, UserProfile b) {
        return accepts(a.preference(), b.gender()) && accepts(b.preference(), a.gender());
    }

    private boolean accepts(Preference preference, Gender gender) {
        if (preference == Preference.BOTH) return true;
        return (preference == Preference.MALE   && gender == Gender.MALE)
            || (preference == Preference.FEMALE && gender == Gender.FEMALE);
    }
}
