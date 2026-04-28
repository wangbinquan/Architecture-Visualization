package com.archvis.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SyncStatusWebSocketHandler extends TextWebSocketHandler {

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.debug("WebSocket closed: {}", session.getId());
    }

    public void broadcast(SyncEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            broadcast(json);
        } catch (IOException e) {
            log.error("Failed to serialize SyncEvent", e);
        }
    }

    public void broadcast(String message) {
        sessions.removeIf(s -> !s.isOpen());
        TextMessage msg = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(msg);
            } catch (IOException e) {
                log.warn("Failed to send to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
