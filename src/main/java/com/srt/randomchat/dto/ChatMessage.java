package com.srt.randomchat.dto;

public record ChatMessage(String roomId, String senderId, String message) {
}
