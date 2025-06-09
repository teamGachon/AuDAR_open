package pednav.backend.pednav.dto;

public record Case1DangerRequest(
        Long timestamp,
        Double radarDetected,   // ESP32 local 연산 결과
        Double soundDetected    // Android local 연산 결과
) {}