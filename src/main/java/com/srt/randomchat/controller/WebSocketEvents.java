package com.srt.randomchat.controller;

import com.srt.randomchat.dto.MatchEvent;
import com.srt.randomchat.dto.SystemEvent;
import com.srt.randomchat.model.MatchOutcome;
import com.srt.randomchat.model.MatchResult;
import com.srt.randomchat.service.MatchService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEvents {

    private final MatchService matchService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEvents(MatchService matchService, SimpMessagingTemplate messagingTemplate) {
        this.matchService      = matchService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) return;

        String roomId = matchService.getRoom(sessionId).orElse(null);
        matchService.handleDisconnect(sessionId).ifPresent(partnerId -> {
            messagingTemplate.convertAndSend("/topic/match/" + partnerId, new MatchEvent("PARTNER_LEFT", roomId));
            matchService.requestMatch(partnerId)
                    .ifPresent(this::notifyMatched);
        });
    }

    private void notifyMatched(MatchOutcome outcome) {
        MatchResult matchResult = outcome.matchResult();
        if (outcome.replacedSessionId() != null && outcome.replacedRoomId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/match/" + outcome.replacedSessionId(),
                    new MatchEvent("PARTNER_LEFT", outcome.replacedRoomId())
            );
        }
        MatchEvent matched = new MatchEvent("MATCHED", matchResult.roomId());
        messagingTemplate.convertAndSend("/topic/match/" + matchResult.sessionA(), matched);
        messagingTemplate.convertAndSend("/topic/match/" + matchResult.sessionB(), matched);
        messagingTemplate.convertAndSend(
                "/topic/room/" + matchResult.roomId(),
                new SystemEvent("SYSTEM", "Match found. Say hi!")
        );
    }
}
