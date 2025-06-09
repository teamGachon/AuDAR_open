package pednav.backend.pednav.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pednav.backend.pednav.dto.OffloadingDecisionRequest;
import pednav.backend.pednav.service.OffloadingDecisionService;

@RestController
@RequestMapping("/api/v1/offloading")
@RequiredArgsConstructor
public class OffloadingDecisionController {

    private final OffloadingDecisionService decisionService;

    @PostMapping("/decide")
    public ResponseEntity<String> decide(@RequestBody OffloadingDecisionRequest request) {
        boolean shouldOffload = decisionService.shouldOffload(request);
        return ResponseEntity.ok(shouldOffload ? "OFFLOAD" : "LOCAL");
    }
}
