package kg.freelance.websocket;

import kg.freelance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    // Track connected users: userId -> sessionId
    private final Map<Long, String> connectedUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof UserPrincipal user) {
                String sessionId = headerAccessor.getSessionId();
                connectedUsers.put(user.getId(), sessionId);
                log.info("User connected: {} (session: {})", user.getEmail(), sessionId);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof UserPrincipal user) {
                connectedUsers.remove(user.getId());
                log.info("User disconnected: {} (session: {})", user.getEmail(), headerAccessor.getSessionId());
            }
        }
    }

    public boolean isUserOnline(Long userId) {
        return connectedUsers.containsKey(userId);
    }

    public String getUserSessionId(Long userId) {
        return connectedUsers.get(userId);
    }
}
