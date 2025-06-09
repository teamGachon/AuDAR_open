
package pednav.backend.pednav.websocket;

import org.springframework.stereotype.Component;
import pednav.backend.pednav.dto.Case3DangerRequest;
import pednav.backend.pednav.service.Case3DangerService;

@Component
public class Case3SensorProcessor {

    private final Case3DangerService case3Service;
    private final UnifiedWebSocketHandler webSocketHandler;

    public Case3SensorProcessor(Case3DangerService case3Service, UnifiedWebSocketHandler webSocketHandler) {
        this.case3Service = case3Service;
        this.webSocketHandler = webSocketHandler;
    }

    public void process(Long timestamp, Case3SensorBuffer.SensorPayload payload) {
        Case3DangerRequest data = new Case3DangerRequest(timestamp);
        data.setSoundDetected(payload.soundDetected.doubleValue());
        data.setVelocity(payload.velocity.doubleValue());
        data.setDistance(payload.distance.doubleValue());

        String danger = case3Service.evaluateAndSaveDanger(data);

        String resultJson = "{\"danger\":\"" + danger + "\"}";
        webSocketHandler.sendToDevice("ANDROID", resultJson);
    }
}
