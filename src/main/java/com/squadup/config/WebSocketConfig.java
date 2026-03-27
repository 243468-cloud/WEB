package com.squadup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Configuración de WebSocket con STOMP.
 *
 * Canales:
 * /topic/lobby/{id} → chat grupal del lobby
 * /user/{id}/queue/notify → notificaciones personales del usuario
 * /user/{id}/queue/dm → mensajes directos 1:1
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefijo de destinos que el cliente envía al servidor
        registry.setApplicationDestinationPrefixes("/app");
        // Broker en memoria: topic (1:N) y queue (1:1)
        registry.enableSimpleBroker("/topic", "/user");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback para navegadores sin WS nativo
    }
}
