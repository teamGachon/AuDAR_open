package pednav.backend.pednav.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class DetectionResult {

    @Id
    @GeneratedValue
    private Long id;

    private Long timestamp;

    // ✅ Case3, Case4
    private Double velocity;
    private Double distance;

    // ✅ Case1, Case2
    private Double radarDetected;

    // ✅ Case1, Case3
    private Double soundDetected;

    // ✅ Case2, Case4 (raw audio 저장용 S3 경로)
    private String audioFileUrl;

    // ✅ Case1~3은 서버에서 직접 결정. Case4는 추후 오프라인 분석
    private String danger;
}
