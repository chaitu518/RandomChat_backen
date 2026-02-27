package com.srt.samples;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class EchoController {

    @MessageMapping("/echo")
    @SendTo("/topic/echo")
    public EchoResponse echo(EchoRequest request) {
        String message = request == null || request.message() == null ? "" : request.message();
        return new EchoResponse(message);
    }
}
