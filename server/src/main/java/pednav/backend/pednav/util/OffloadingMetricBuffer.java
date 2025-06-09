
package pednav.backend.pednav.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pednav.backend.pednav.dto.OffloadingDecisionRequest;
import pednav.backend.pednav.service.OffloadingDecisionService;
import pednav.backend.pednav.websocket.UnifiedWebSocketHandler;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class OffloadingMetricBuffer {

    public static class MetricPayload {
        public OffloadingDecisionRequest androidMetric;
        public OffloadingDecisionRequest esp32Metric;
    }

    private final ConcurrentHashMap<Long, MetricPayload> buffer = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final SlidingWindowMatcher<MetricPayload> matcher = new SlidingWindowMatcher<>(1000); // 1000ms 기준

    private final OffloadingDecisionService decisionService;
    private final UnifiedWebSocketHandler webSocketHandler;

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::flush, 0, 1, TimeUnit.SECONDS);
    }

    public void putMetric(OffloadingDecisionRequest req) {
        long timestamp = req.timestamp();

        buffer.compute(timestamp, (ts, existing) -> {
            if (existing == null) existing = new MetricPayload();
            if ("ANDROID".equalsIgnoreCase(req.deviceType())) existing.androidMetric = req;
            else if ("ESP32".equalsIgnoreCase(req.deviceType())) existing.esp32Metric = req;
            return existing;
        });
    }

    private void flush() {
        matcher.match(
                buffer,
                // onMatch: 제거
                (ts1, ts2) -> {
                    buffer.remove(ts1);
                    buffer.remove(ts2);
                },
                // merge: 병합 후 판단
                (p1, p2) -> {
                    OffloadingDecisionRequest android = (p1.androidMetric != null) ? p1.androidMetric : p2.androidMetric;
                    OffloadingDecisionRequest esp32 = (p1.esp32Metric != null) ? p1.esp32Metric : p2.esp32Metric;

                    if (android != null && esp32 != null) {
                        int caseId = decisionService.determineCase(android, esp32);
                        webSocketHandler.sendToDevice("ANDROID", "{\"case\": " + caseId + "}");
                        webSocketHandler.sendToDevice("ESP32", "{\"case\": " + caseId + "}");
                    }
                },
                // 매칭 조건: androidMetric, esp32Metric 둘 다 존재하는 쌍
                (p1, p2) -> {
                    boolean case1 = p1.androidMetric != null && p2.esp32Metric != null;
                    boolean case2 = p2.androidMetric != null && p1.esp32Metric != null;
                    return case1 || case2;
                }
        );
    }
}