package pednav.backend.pednav.websocket;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import pednav.backend.pednav.util.SlidingWindowMatcher;

import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Case4SensorBuffer {

    public static class SensorPayload {
        public byte[] audioPcm;
        public Float velocity;
        public Float distance;
    }

    private final ConcurrentHashMap<Long, SensorPayload> buffer = new ConcurrentHashMap<>();
    private final Case4SensorProcessor processor;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final SlidingWindowMatcher<SensorPayload> matcher = new SlidingWindowMatcher<>(400);

    public Case4SensorBuffer(Case4SensorProcessor processor) {
        this.processor = processor;
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::flush, 0, 1, TimeUnit.SECONDS);
    }

    public void putAudioData(long timestamp, byte[] audioPcm) {
        buffer.compute(timestamp, (ts, existing) -> {
            if (existing == null) existing = new SensorPayload();
            existing.audioPcm = audioPcm;
            return existing;
        });
    }

    public void putVelocityDistance(long timestamp, float velocity, float distance) {
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
                (ts1, ts2) -> {
                    buffer.remove(ts1);
                    buffer.remove(ts2);
                },
                (p1, p2) -> {
                    SensorPayload merged = new SensorPayload();
                    merged.audioPcm = (p1.audioPcm != null) ? p1.audioPcm : p2.audioPcm;
                    merged.velocity = (p1.velocity != null) ? p1.velocity : p2.velocity;
                    merged.distance = (p1.distance != null) ? p1.distance : p2.distance;

                    long mergedTs = System.currentTimeMillis();
                    processor.process(mergedTs, merged);
                },
                (p1, p2) -> {
                    boolean case1 = p1.audioPcm != null && p2.velocity != null && p2.distance != null;
                    boolean case2 = p2.audioPcm != null && p1.velocity != null && p1.distance != null;
                    return case1 || case2;
                }
        );
    }
}
