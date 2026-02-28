package com.srt.randomchat.bot;

import com.srt.randomchat.dto.ChatMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class WebSocketBotIntegrationSnippet {

    private final BotService botService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketBotIntegrationSnippet(BotService botService, SimpMessagingTemplate messagingTemplate) {
        this.botService = botService;
        this.messagingTemplate = messagingTemplate;
    }

    public void onUserMessage(String sessionId, String roomId, String userMessage) {
        botService.generateReply(sessionId, userMessage)
                .thenAccept(reply -> messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        new ChatMessage(roomId, "bot", reply)
                ));
    }
}
