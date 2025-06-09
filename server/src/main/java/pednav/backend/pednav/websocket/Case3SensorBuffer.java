package pednav.backend.pednav.websocket;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import pednav.backend.pednav.util.SlidingWindowMatcher;

import java.util.concurrent.*;

@Component
public class Case3SensorBuffer {

    public static class SensorPayload {
        public Float soundDetected;  // from Android
        public Float velocity, distance;  // from ESP32
    }

    private final ConcurrentHashMap<Long, SensorPayload> buffer = new ConcurrentHashMap<>();
    private final Case3SensorProcessor processor;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final SlidingWindowMatcher<SensorPayload> matcher = new SlidingWindowMatcher<>(400); // 400ms

    public Case3SensorBuffer(Case3SensorProcessor processor) {
        this.processor = processor;
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::flush, 0, 1, TimeUnit.SECONDS);
    }

    public void putAndroidData(long timestamp, float soundDetected) {
        buffer.compute(timestamp, (ts, existing) -> {
            if (existing == null) existing = new SensorPayload();
            existing.soundDetected = soundDetected;
            return existing;
        });
    }

    public void putESP32Data(long timestamp, float velocity, float distance) {
        buffer.compute(timestamp, (ts, existing) -> {
            if (existing == null) existing = new SensorPayload();
            existing.velocity = velocity;
            existing.distance = distance;
            return existing;
        });
    }

    private void flush() {
        matcher.match(
                buffer,
                // onMatch: 매칭된 두 timestamp 제거
                (ts1, ts2) -> {
                    buffer.remove(ts1);
                    buffer.remove(ts2);
                },
                // matchProcessor: 병합 후 처리
                (p1, p2) -> {
                    SensorPayload merged = new SensorPayload();
                    merged.soundDetected = (p1.soundDetected != null) ? p1.soundDetected : p2.soundDetected;
                    merged.velocity = (p1.velocity != null) ? p1.velocity : p2.velocity;
                    merged.distance = (p1.distance != null) ? p1.distance : p2.distance;

                    long mergedTs = System.currentTimeMillis(); // 또는 ts1/ts2 중 최소
                    processor.process(mergedTs, merged);
                },
                // matchCondition: 한 쪽은 vehicleDetected, 다른 쪽은 velocity + distance 있어야 함
                (p1, p2) -> {
                    boolean case1 = p1.soundDetected != null && p2.velocity != null && p2.distance != null;
                    boolean case2 = p2.soundDetected != null && p1.velocity != null && p1.distance != null;
                    return case1 || case2;
                }
        );
    }
}
