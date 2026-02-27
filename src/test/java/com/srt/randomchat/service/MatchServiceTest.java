package com.srt.randomchat.service;

import com.srt.randomchat.model.Gender;
import com.srt.randomchat.model.MatchResult;
import com.srt.randomchat.model.Preference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchServiceTest {

    @Test
    void matchesCompatibleUsers() {
        MatchService service = new MatchService();
        service.register("a", Gender.MALE, Preference.FEMALE);
        service.register("b", Gender.FEMALE, Preference.MALE);

        assertTrue(service.requestMatch("a").isEmpty());
        MatchResult result = service.requestMatch("b").orElse(null);

        assertNotNull(result);
        assertTrue(result.roomId().length() > 0);
    }

    @Test
    void doesNotMatchIncompatibleUsers() {
        MatchService service = new MatchService();
        service.register("a", Gender.MALE, Preference.FEMALE);
        service.register("b", Gender.MALE, Preference.BOTH);

        assertTrue(service.requestMatch("a").isEmpty());
        assertTrue(service.requestMatch("b").isEmpty());
        assertFalse(service.getRoom("a").isPresent());
        assertFalse(service.getRoom("b").isPresent());
    }
}
