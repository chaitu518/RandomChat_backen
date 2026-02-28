package com.srt.randomchat.bot;

record OllamaGenerateRequest(String model, String prompt, boolean stream, OllamaOptions options) {
}

record OllamaOptions(Integer num_predict, Double temperature) {
}

record OllamaGenerateResponse(String response) {
}
