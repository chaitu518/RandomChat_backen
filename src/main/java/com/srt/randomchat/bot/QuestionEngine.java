package com.srt.randomchat.bot;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class QuestionEngine {

    private final List<String> questions = List.of(
            "Beach or mountains?",
            "Introvert or extrovert?",
            "What do you do?",
            "How's your day?",
            "Music or movies?"
    );

    public String randomQuestion() {
        return questions.get(ThreadLocalRandom.current().nextInt(questions.size()));
    }
}
