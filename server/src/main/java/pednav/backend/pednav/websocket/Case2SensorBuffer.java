package pednav.backend.pednav.websocket;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import pednav.backend.pednav.util.SlidingWindowMatcher;

import java.util.concurrent.*;

@Component
public class Case2SensorBuffer {

    public static class SensorPayload {
        public Float radarDetected;      // from ESP32
        public byte[] audioPcm;          // from Android
    }

    private final ConcurrentHashMap<Long, SensorPayload> buffer = new ConcurrentHashMap<>();
    private final Case2SensorProcessor processor;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final SlidingWindowMatcher<SensorPayload> matcher = new SlidingWindowMatcher<>(400); // 400ms window

    public Case2SensorBuffer(Case2SensorProcessor processor) {
        this.processor = processor;
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::flush, 0, 1, TimeUnit.SECONDS);
    }

    public void putRadarData(long timestamp, float radarDetected) {
        buffer.compute(timestamp, (ts, existing) -> {
            if (existing == null) existing = new SensorPayload();
            existing.radarDetected = radarDetected;
            return existing;
        });
    }

    public void putAudioData(long timestamp, byte[] audioPcm) {
        buffer.compute(timestamp, (ts, existing) -> {
            if (existing == null) existing = new SensorPayload();
            existing.audioPcm = audioPcm;
            return existing;
        });
    }

    private void flush() {
        matcher.match(
                buffer,
                (ts1, ts2) -> {
                    buffer.remove(ts1);
                    buffer.remove(ts2);
                },
                (p1, p2) -> {
                    SensorPayload merged = new SensorPayload();
                    merged.radarDetected = (p1.radarDetected != null) ? p1.radarDetected : p2.radarDetected;
                    merged.audioPcm = (p1.audioPcm != null) ? p1.audioPcm : p2.audioPcm;

                    long mergedTs = System.currentTimeMillis(); // or Math.min(ts1, ts2);
                    processor.process(mergedTs, merged);
                },
                (p1, p2) -> {
                    boolean hasRadar = (p1.radarDetected != null && p2.audioPcm != null);
                    boolean hasRadarAlt = (p2.radarDetected != null && p1.audioPcm != null);
                    return hasRadar || hasRadarAlt;
                }
        );
    }
}
