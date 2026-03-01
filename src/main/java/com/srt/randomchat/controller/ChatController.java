package com.srt.randomchat.controller;

import com.srt.randomchat.bot.BotService;
import com.srt.randomchat.dto.ChatMessage;
import com.srt.randomchat.dto.HelloRequest;
import com.srt.randomchat.dto.JoinRequest;
import com.srt.randomchat.dto.MatchEvent;
import com.srt.randomchat.dto.SendMessageRequest;
import com.srt.randomchat.dto.SessionEvent;
import com.srt.randomchat.dto.SystemEvent;
import com.srt.randomchat.model.MatchOutcome;
import com.srt.randomchat.model.MatchResult;
import com.srt.randomchat.service.MatchService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    private final MatchService matchService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BotService botService;

    public ChatController(MatchService matchService, SimpMessagingTemplate messagingTemplate, BotService botService) {
        this.matchService      = matchService;
        this.messagingTemplate = messagingTemplate;
        this.botService = botService;
    }


    @MessageMapping("/hello")
    public void hello(HelloRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null || request == null || request.clientId() == null || request.clientId().isBlank()) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/hello", new SessionEvent(request.clientId(), sessionId));
    }

    @MessageMapping("/join")
    public void join(JoinRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) return;
        if (request == null || request.gender() == null || request.preference() == null) {
            sendError(sessionId, "Invalid join payload. Provide gender and preference.");
            return;
        }
        String anonymousId = matchService.register(sessionId, request.gender(), request.preference());
        messagingTemplate.convertAndSend("/topic/system/" + sessionId, new SystemEvent("IDENTITY", anonymousId));
        matchService.requestMatch(sessionId)
                .ifPresent(this::notifyMatched);
    }

    @MessageMapping("/message")
    public void message(SendMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) return;
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
                            if (matchService.isBotRoom(roomId) && botService.isEnabled()) {
                                messagingTemplate.convertAndSend(
                                        "/topic/room/" + roomId,
                                        new SystemEvent("TYPING", "typing...")
                                );
                                botService.generateReply(sessionId, request.message())
                                        .thenAccept(reply -> {
                                            if (reply == null) {
                                                handleBotFailure(sessionId, roomId);
                                                return;
                                            }
                                            messagingTemplate.convertAndSend(
                                                    "/topic/room/" + roomId,
                                                    new ChatMessage(roomId, botService.getBotSenderId(), reply)
                                            );
                                        });
                            }
                        },
                        () -> sendError(sessionId, "You are not in this room.")
                );
    }

    @MessageMapping("/next")
    public void next(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) return;
        if (!matchService.isRegistered(sessionId)) {
            sendError(sessionId, "Join first before requesting next match.");
            return;
        }

        String roomId = matchService.getRoom(sessionId).orElse(null);
        matchService.leaveRoom(sessionId).ifPresent(partnerId -> {
            messagingTemplate.convertAndSend("/topic/match/" + partnerId, new MatchEvent("PARTNER_NEXT", roomId));
            matchService.requestMatch(partnerId)
                    .ifPresent(this::notifyMatched);
        });

        matchService.requestMatch(sessionId)
                .ifPresent(this::notifyMatched);
    }

    @MessageMapping("/leave")
    public void leave(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) return;
        if (!matchService.isRegistered(sessionId)) {
            sendError(sessionId, "Join first before leaving.");
            return;
        }

        matchService.cancelSearch(sessionId);
        String roomId = matchService.getRoom(sessionId).orElse(null);
        matchService.leaveRoom(sessionId).ifPresent(partnerId ->
                messagingTemplate.convertAndSend("/topic/match/" + partnerId, new MatchEvent("PARTNER_LEFT", roomId))
        );
    }

    @MessageMapping("/online-count")
    public void onlineCount() {
        int count = matchService.getConnectedCount();
        messagingTemplate.convertAndSend("/topic/online-count", count);
    }

    private void sendError(String sessionId, String message) {
        messagingTemplate.convertAndSend("/topic/system/" + sessionId, new SystemEvent("ERROR", message));
    }

    private void notifyMatched(MatchOutcome outcome) {
        MatchResult matchResult = outcome.matchResult();
        if (outcome.replacedSessionId() != null && outcome.replacedRoomId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/match/" + outcome.replacedSessionId(),
                    new MatchEvent("PARTNER_LEFT", outcome.replacedRoomId())
            );
        }
        MatchEvent event = new MatchEvent("MATCHED", matchResult.roomId());
        messagingTemplate.convertAndSend("/topic/match/" + matchResult.sessionA(), event);
        messagingTemplate.convertAndSend("/topic/match/" + matchResult.sessionB(), event);
        messagingTemplate.convertAndSend(
                "/topic/room/" + matchResult.roomId(),
                new SystemEvent("SYSTEM", "Match found. Say hi!")
        );
    }

    private void handleBotFailure(String sessionId, String roomId) {
        matchService.leaveRoom(sessionId);
        messagingTemplate.convertAndSend(
                "/topic/match/" + sessionId,
                new MatchEvent("PARTNER_LEFT", roomId)
        );
        matchService.requestMatch(sessionId)
                .ifPresent(this::notifyMatched);
        messagingTemplate.convertAndSend(
                "/topic/system/" + sessionId,
                new SystemEvent("ERROR", "Bot unavailable. Searching for a partner...")
        );
    }

}
