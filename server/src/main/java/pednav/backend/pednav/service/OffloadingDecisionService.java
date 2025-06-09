package pednav.backend.pednav.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pednav.backend.pednav.dto.OffloadingDecisionRequest;
import pednav.backend.pednav.util.OffloadingMetricStore;
import pednav.backend.pednav.websocket.UnifiedWebSocketHandler;

@Service
@RequiredArgsConstructor
public class OffloadingDecisionService {

    private final OffloadingMetricStore metricStore;
    private final UnifiedWebSocketHandler webSocketHandler;

    // 가중치 상수 (게임이론 기반)
    private static final double CPU_WEIGHT = 0.6;
    private static final double BATTERY_WEIGHT = 0.4;
    private static final double LATENCY_WEIGHT = 0.7;

    public void receiveMetric(OffloadingDecisionRequest req) {
        metricStore.save(req);
        System.out.println("📥 Metric 저장됨: " + req);


        if (metricStore.isComplete()) {
            System.out.println("🧠 Metric 쌍이 모두 도착함");
            OffloadingDecisionRequest androidMetric = metricStore.getAndroidMetric();
            OffloadingDecisionRequest esp32Metric = metricStore.getEsp32Metric();

            int caseId = determineCase(androidMetric, esp32Metric);

            webSocketHandler.sendToDevice("ANDROID", "{\"case\": " + caseId + "}");
            webSocketHandler.sendToDevice("ESP32", "{\"case\": " + caseId + "}");

            System.out.println("📤 WebSocket Case 전송 완료 → Case " + caseId);

            metricStore.clear();
        }
    }

    public int determineCase(OffloadingDecisionRequest android, OffloadingDecisionRequest esp32) {
        boolean androidOffload = shouldOffload(android);
        boolean esp32Offload = shouldOffload(esp32);

        System.out.println("🧠 [Case 판단 로직]");
        System.out.println("📱 Android - CPU: " + android.cpuLoad() + ", Battery: " + android.batteryLevel() + ", Latency: " + android.latency());
        System.out.println("📡 ESP32 - CPU: " + esp32.cpuLoad() + ", Battery: " + esp32.batteryLevel() + ", Latency: " + esp32.latency());
        System.out.println("➡️  Offload 결정 → Android: " + androidOffload + ", ESP32: " + esp32Offload);


        if (!androidOffload && !esp32Offload) return 1;
        else if (androidOffload && !esp32Offload) return 2;
        else if (!androidOffload && esp32Offload) return 3;
        else return 4;
    }

    public boolean shouldOffload(OffloadingDecisionRequest req) {
        double uLocal = calculateLocalUtility(req);
        double uOffload = calculateOffloadUtility(req);

        System.out.println("🔎 shouldOffload() - Local: " + uLocal + ", Offload: " + uOffload);

        return uOffload > uLocal;
    }

    private double calculateLocalUtility(OffloadingDecisionRequest req) {
        double cpuScore = 1.0 - req.cpuLoad(); //낮을수록 유리
        double batteryScore = req.batteryLevel(); //높을수록 유리
        double score = CPU_WEIGHT * cpuScore + BATTERY_WEIGHT * batteryScore;

        System.out.println("📊 Local Utility - CPU Score: " + cpuScore + ", Battery Score: " + batteryScore + ", Total: " + score);
        return score;
    }

    private double calculateOffloadUtility(OffloadingDecisionRequest req) {
        double latencyScore = 1.0 - (req.latency() / 1000.0);
        latencyScore = Math.max(0, Math.min(latencyScore, 1.0));
        double score = LATENCY_WEIGHT * latencyScore;

        System.out.println("📡 Offload Utility - Latency Score: " + latencyScore + ", Total: " + score);
        return score;
    }
}
