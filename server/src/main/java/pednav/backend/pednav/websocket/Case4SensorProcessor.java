package pednav.backend.pednav.websocket;

import org.springframework.stereotype.Component;
import pednav.backend.pednav.service.Case4DangerService;

@Component
public class Case4SensorProcessor {

    private final Case4DangerService dangerService;
    private final UnifiedWebSocketHandler webSocketHandler;

    public Case4SensorProcessor(Case4DangerService dangerService, UnifiedWebSocketHandler webSocketHandler) {
        this.dangerService = dangerService;
        this.webSocketHandler = webSocketHandler;
    }

    public void process(Long timestamp, Case4SensorBuffer.SensorPayload payload) {
        if (payload.audioPcm == null || payload.velocity == null || payload.distance == null) return;

        String danger = dangerService.evaluateAndSaveDanger(timestamp,
                payload.velocity.doubleValue(),
                payload.distance.doubleValue(),
                payload.audioPcm
        );

        String resultJson = "{\"danger\":\"" + danger + "\"}";
        webSocketHandler.sendToDevice("ANDROID", resultJson);
    }
}
