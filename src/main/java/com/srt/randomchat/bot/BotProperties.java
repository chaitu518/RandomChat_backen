package com.srt.randomchat.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "randomchat.bot")
public class BotProperties {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:11434";
    private String model = "gemma3:4b";
    private int maxTokens = 128;
    private double temperature = 0.7;

    private String name = "Mia";
    private int age = 23;
    private String place = "Mumbai";
    private String persona = "Friendly and natural";

    private int memoryLimit = 12;
    private int maxNoQuestionTurns = 2;
    private double similarityThreshold = 0.8;
    private int maxWords = 4;
    private int unavailableSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public int getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(int memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public int getMaxNoQuestionTurns() {
        return maxNoQuestionTurns;
    }

    public void setMaxNoQuestionTurns(int maxNoQuestionTurns) {
        this.maxNoQuestionTurns = maxNoQuestionTurns;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public int getMaxWords() {
        return maxWords;
    }

    public void setMaxWords(int maxWords) {
        this.maxWords = maxWords;
    }

    public int getUnavailableSeconds() {
        return unavailableSeconds;
    }

    public void setUnavailableSeconds(int unavailableSeconds) {
        this.unavailableSeconds = unavailableSeconds;
    }
}
