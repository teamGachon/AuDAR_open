package pednav.backend.pednav.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pednav.backend.pednav.repository.DataRepository;

@Service
@RequiredArgsConstructor
public class CleanupService {

    private final DataRepository repository;

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void deleteOldData() {
        long now = System.currentTimeMillis();
        long threshold = now - (1 * 60 * 1000); // 1분 전 기준
        repository.deleteOldEntries(threshold);
        System.out.println("🧹 1분 이상 지난 데이터 삭제 완료");
    }
}
