package com.srt.randomchat.aitest;

import com.srt.randomchat.bot.BotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AiTestController {

    private final BotService botService;

    public AiTestController(BotService botService) {
        this.botService = botService;
    }

    @GetMapping("/ai-test")
    public Map<String, String> testAI() {
        String reply = botService.generate("Say hello casually");
        return Map.of("reply", reply);
    }
}