package pednav.backend.pednav.dto;

// Case2DangerRequest.java
public record Case2DangerRequest(
        Long timestamp,
        Double radarDetected,
        String danger,
        String audioFileUrl // 추후 S3 저장 시 사용 가능
) {}
