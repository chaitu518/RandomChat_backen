package com.srt.randomchat.controller;

import com.srt.randomchat.dto.ChatMessage;
import com.srt.randomchat.dto.HelloRequest;
import com.srt.randomchat.dto.JoinRequest;
import com.srt.randomchat.dto.MatchEvent;
import com.srt.randomchat.dto.SendMessageRequest;
import com.srt.randomchat.dto.SessionEvent;
import com.srt.randomchat.dto.SystemEvent;
import com.srt.randomchat.model.MatchResult;
import com.srt.randomchat.service.MatchService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final MatchService matchService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(MatchService matchService, SimpMessagingTemplate messagingTemplate) {
        this.matchService = matchService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/hello")
    public void hello(HelloRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null || request == null || request.clientId() == null || request.clientId().isBlank()) {
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/hello",
                new SessionEvent(request.clientId(), sessionId)
        );
    }

    @MessageMapping("/join")
    public void join(JoinRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            return;
        }
        if (request == null || request.gender() == null || request.preference() == null) {
            sendError(sessionId, "Invalid join payload. Provide gender and preference.");
            return;
        }

        String anonymousId = matchService.register(sessionId, request.gender(), request.preference());
        messagingTemplate.convertAndSend(
                "/topic/system/" + sessionId,
                new SystemEvent("IDENTITY", anonymousId)
        );
        matchService.requestMatch(sessionId).ifPresent(this::notifyMatched);
    }

    @MessageMapping("/message")
    public void message(SendMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            return;
        }
        if (!matchService.isRegistered(sessionId)) {
            sendError(sessionId, "Join first before sending messages.");
            return;
        }
        if (request == null || request.roomId() == null || request.message() == null || request.message().isBlank()) {
            sendError(sessionId, "Invalid message payload.");
            return;
        }

        matchService.getRoom(sessionId)
                .filter(roomId -> roomId.equals(request.roomId()))
                .ifPresentOrElse(
                        roomId -> {
                            String senderId = matchService.getAnonymousId(sessionId).orElse("anon");
                            messagingTemplate.convertAndSend(
                                    "/topic/room/" + roomId,
                                    new ChatMessage(roomId, senderId, request.message())
                            );
                        },
                        () -> sendError(sessionId, "You are not in this room.")
                );
    }

    @MessageMapping("/next")
    public void next(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            return;
        }
        if (!matchService.isRegistered(sessionId)) {
            sendError(sessionId, "Join first before requesting next match.");
            return;
        }

        String roomId = matchService.getRoom(sessionId).orElse(null);
        matchService.leaveRoom(sessionId).ifPresent(partnerId -> {
            messagingTemplate.convertAndSend(
                    "/topic/match/" + partnerId,
                    new MatchEvent("PARTNER_LEFT", roomId)
            );
            matchService.requestMatch(partnerId).ifPresent(this::notifyMatched);
        });

        matchService.requestMatch(sessionId).ifPresent(this::notifyMatched);
    }

    private void sendError(String sessionId, String message) {
        messagingTemplate.convertAndSend(
                "/topic/system/" + sessionId,
                new SystemEvent("ERROR", message)
        );
    }

    private void notifyMatched(MatchResult matchResult) {
        MatchEvent event = new MatchEvent("MATCHED", matchResult.roomId());
        messagingTemplate.convertAndSend("/topic/match/" + matchResult.sessionA(), event);
        messagingTemplate.convertAndSend("/topic/match/" + matchResult.sessionB(), event);
        messagingTemplate.convertAndSend(
                "/topic/room/" + matchResult.roomId(),
                new SystemEvent("SYSTEM", "Match found. Say hi!")
        );
    }
}
