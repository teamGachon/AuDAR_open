package pednav.backend.pednav.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pednav.backend.pednav.domain.entity.DetectionResult;

public interface DataRepository extends JpaRepository<DetectionResult, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM DetectionResult d WHERE d.timestamp < :threshold")
    void deleteOldEntries(@Param("threshold") Long threshold);
}

