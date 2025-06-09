package pednav.backend.pednav.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pednav.backend.pednav.domain.entity.DetectionResult;
import pednav.backend.pednav.external.FastApiClient;
import pednav.backend.pednav.repository.DataRepository;
import pednav.backend.pednav.util.S3Uploader;

@Service
@RequiredArgsConstructor
public class Case4DangerService {

    private final DataRepository repository;
    private final FastApiClient fastApiClient;
    private final S3Uploader s3Uploader;

    public String evaluateAndSaveDanger(Long timestamp, Double velocity, Double distance, byte[] audioPcm) {
        String audioUrl = s3Uploader.uploadPcm(audioPcm, "case4-audio");
        String danger = fastApiClient.predictDangerCase4(velocity, distance, audioPcm);

        DetectionResult entity = new DetectionResult();
        entity.setTimestamp(timestamp);
        entity.setVelocity(velocity);
        entity.setDistance(distance);
        entity.setAudioFileUrl(audioUrl);
        entity.setDanger(danger);

        repository.save(entity);
        System.out.println("Case4 : ✅ DB 저장 완료 - danger: " + danger);
        return danger;
    }
}
