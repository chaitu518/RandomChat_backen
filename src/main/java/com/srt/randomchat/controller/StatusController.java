package com.srt.randomchat.controller;

import com.srt.randomchat.model.MatchStats;
import com.srt.randomchat.service.MatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final MatchService matchService;

    public StatusController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        MatchStats stats = matchService.getStats();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app", "randomchat");
        payload.put("registered", stats.registeredCount());
        payload.put("waiting", stats.waitingCount());
        payload.put("activeRooms", stats.activeRooms());
        return payload;
    }
}
