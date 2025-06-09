package pednav.backend.pednav.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pednav.backend.pednav.domain.entity.DetectionResult;
import pednav.backend.pednav.external.FastApiClient;
import pednav.backend.pednav.repository.DataRepository;
import pednav.backend.pednav.util.S3Uploader;

@Service
@RequiredArgsConstructor
public class Case2DangerService {

    private final DataRepository repository;
    private final FastApiClient fastApiClient;
    private final S3Uploader s3Uploader; // ✅ S3Uploader 주입

    public String evaluateAndSaveDanger(Long timestamp, Double radarDetected, byte[] audioPcm) {
        // ✅ 1. S3 업로드
        String audioUrl = s3Uploader.uploadPcm(audioPcm, "case2-audio");

        // ✅ 2. FastAPI (8002) 서버로 danger 예측 요청
        String danger = fastApiClient.predictDangerCase2(radarDetected, audioPcm);

        // ✅ 3. DB에 저장
        DetectionResult entity = new DetectionResult();
        entity.setTimestamp(timestamp);
        entity.setRadarDetected(radarDetected);
        entity.setDanger(danger);
        entity.setAudioFileUrl(audioUrl); // ✅ 저장된 S3 경로
        entity.setVelocity(null);         // Case2는 사용 안 함
        entity.setDistance(null);
        entity.setSoundDetected(null);

        repository.save(entity);
        System.out.println("Case2 : ✅ DB 저장 완료 - danger: " + danger);

        return danger;
    }
}
