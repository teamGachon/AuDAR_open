package pednav.backend.pednav.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pednav.backend.pednav.dto.Case3DangerRequest;
import pednav.backend.pednav.external.FastApiClient;
import pednav.backend.pednav.repository.DataRepository;

@Service
@RequiredArgsConstructor
public class Case3DangerService {

    private final DataRepository repository;
    private final FastApiClient fastApiClient;

    public String evaluateAndSaveDanger(Case3DangerRequest data) {
        String danger = fastApiClient.predictDangerCase3(data);
        data.setDanger(danger);
        repository.save(data.toEntity());
        System.out.println("Case3 : ✅ DB 저장 완료 - danger: " + danger);

        return danger;
    }
}
