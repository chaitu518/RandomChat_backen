package com.srt.randomchat.model;

public record MatchOutcome(MatchResult matchResult, String replacedSessionId, String replacedRoomId) {
}
