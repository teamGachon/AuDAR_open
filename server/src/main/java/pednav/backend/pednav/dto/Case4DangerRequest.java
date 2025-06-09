package pednav.backend.pednav.dto;

public record Case4DangerRequest(
        Long timestamp,
        Double velocity,
        Double distance,
        byte[] audioPcm
) {}
