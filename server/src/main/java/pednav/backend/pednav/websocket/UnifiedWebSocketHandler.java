package pednav.backend.pednav.websocket;

import java.util.function.Consumer;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pednav.backend.pednav.dto.OffloadingDecisionRequest;
import pednav.backend.pednav.service.OffloadingDecisionService;
import pednav.backend.pednav.service.SyncService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class UnifiedWebSocketHandler extends TextWebSocketHandler {

    private Consumer<String> messageHandler;
    private final List<WebSocketSession> androidSessions = new CopyOnWriteArrayList<>();
    private final List<WebSocketSession> esp32Sessions = new CopyOnWriteArrayList<>();
    private final OffloadingDecisionService offloadingDecisionService; // ✅ 필드 선언


    @Autowired
    public UnifiedWebSocketHandler(@Lazy OffloadingDecisionService offloadingDecisionService) {
        this.offloadingDecisionService = offloadingDecisionService;
    }
    @Autowired
    public void setMessageHandler(@Lazy SyncService syncService) {
        this.messageHandler = syncService::processIncomingJson;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("✅ WebSocket 연결 수립: " + session.getId());
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        System.out.println("📨 WebSocket 수신됨: " + payload);

        try {
            JSONObject obj = new JSONObject(payload);

            // 🧠 metric 형식이면
            if (obj.has("cpuLoad") && obj.has("batteryLevel") && obj.has("latency")) {
                OffloadingDecisionRequest req = new OffloadingDecisionRequest(
                        obj.getLong("timestamp"),
                        obj.getString("deviceId"),
                        obj.getString("deviceType"),
                        obj.getDouble("latency"),
                        obj.getDouble("cpuLoad"),
                        obj.getDouble("batteryLevel")
                );
                System.out.println("🧩 Metric 인식됨 → " + req);
                offloadingDecisionService.receiveMetric(req); // 🔥 실제 처리 호출
                return;
            }

            // 기존 로직
            if (payload.contains("deviceType")) {
                // 연결 초기 메시지
                if (payload.contains("ANDROID") && !androidSessions.contains(session)) {
                    androidSessions.add(session);
                } else if (payload.contains("ESP32") && !esp32Sessions.contains(session)) {
                    esp32Sessions.add(session);
                }
            } else if (messageHandler != null) {
                messageHandler.accept(payload);
            }

        } catch (Exception e) {
            System.err.println("❌ WebSocket 메시지 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }



    // 각 타입별 메시지 전송
    public void sendToDevice(String deviceType, String json) {
        System.out.println("📤 [" + deviceType + "]에 메시지 전송: " + json);
        List<WebSocketSession> targetSessions = deviceType.equals("ANDROID") ? androidSessions : esp32Sessions;
        for (WebSocketSession session : targetSessions) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
