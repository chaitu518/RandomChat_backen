package com.srt.randomchat.controller;

import com.srt.randomchat.dto.MatchEvent;
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
        this.matchService = matchService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId == null) {
            return;
        }

        String roomId = matchService.getRoom(sessionId).orElse(null);
        matchService.handleDisconnect(sessionId).ifPresent(partnerId -> {
            messagingTemplate.convertAndSend(
                    "/topic/match/" + partnerId,
                    new MatchEvent("PARTNER_LEFT", roomId)
            );
            matchService.requestMatch(partnerId).ifPresent(matchResult -> {
                MatchEvent eventMessage = new MatchEvent("MATCHED", matchResult.roomId());
                messagingTemplate.convertAndSend("/topic/match/" + matchResult.sessionA(), eventMessage);
                messagingTemplate.convertAndSend("/topic/match/" + matchResult.sessionB(), eventMessage);
            });
        });
    }
}
