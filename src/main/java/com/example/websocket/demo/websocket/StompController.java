package com.example.websocket.demo.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class StompController {

    private static final Logger logger = LoggerFactory.getLogger(StompController.class);

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/echo")
    public void echo(String message, Principal userPrincipal) {
        logger.info("received stomp message: {} ({})", message, userPrincipal);
        simpMessagingTemplate.convertAndSendToUser(userPrincipal.getName(), "/topic/echo", message);
    }
}
