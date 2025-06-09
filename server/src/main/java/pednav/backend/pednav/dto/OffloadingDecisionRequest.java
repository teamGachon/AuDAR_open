package pednav.backend.pednav.dto;

public record OffloadingDecisionRequest(
        long timestamp,
        String deviceId,
        String deviceType, // ANDROID or ESP32
        double latency,         // ms
        double cpuLoad,         // 0~1
        double batteryLevel  // 0~1
) {}
