package pednav.backend.pednav.websocket;

import org.springframework.stereotype.Component;
import pednav.backend.pednav.service.Case2DangerService;

@Component
public class Case2SensorProcessor {

    private final Case2DangerService case2Service;
    private final UnifiedWebSocketHandler webSocketHandler;

    public Case2SensorProcessor(Case2DangerService case2Service, UnifiedWebSocketHandler webSocketHandler) {
        this.case2Service = case2Service;
        this.webSocketHandler = webSocketHandler;
    }

    public void process(Long timestamp, Case2SensorBuffer.SensorPayload payload) {
        String danger = case2Service.evaluateAndSaveDanger(timestamp, payload.radarDetected.doubleValue(), payload.audioPcm);

        String resultJson = "{\"danger\":\"" + danger + "\"}";
        webSocketHandler.sendToDevice("ANDROID", resultJson); // ✅ 수정
    }
}
