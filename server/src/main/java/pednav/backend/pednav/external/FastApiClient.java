package pednav.backend.pednav.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import pednav.backend.pednav.dto.Case3DangerRequest;

@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String CASE2_API_URL = "http://3.34.129.82:8002/predict";
    private final String CASE3_API_URL = "http://3.34.129.82:8003/predict";
    private final String CASE4_API_URL = "http://3.34.129.82:8004/predict"; // 나중에

    /**
     * Case3: sound_detected + velocity + distance
     */
    public String predictDangerCase3(Case3DangerRequest data) {
        try {
            JsonNode request = objectMapper.createObjectNode()
                    .put("sound_detected", data.getSoundDetected())
                    .put("velocity", data.getVelocity())
                    .put("distance", data.getDistance());

            String response = restTemplate.postForObject(CASE3_API_URL, request, String.class);
            JsonNode json = objectMapper.readTree(response);
            return json.get("danger").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }

    /**
     * Case2: radar_detected + audio_file (multipart/form-data)
     */
    public String predictDangerCase2(Double radarDetected, byte[] audioPcm) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("radar_detected", radarDetected);
            body.add("audio_file", new ByteArrayResource(audioPcm) {
                @Override
                public String getFilename() {
                    return "audio.pcm";
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(CASE2_API_URL, request, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("danger").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }

    public String predictDangerCase4(Double velocity, Double distance, byte[] audioPcm) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("velocity", velocity);
            body.add("distance", distance);
            body.add("sound_file", new ByteArrayResource(audioPcm) {
                @Override
                public String getFilename() {
                    return "audio.pcm";
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(CASE4_API_URL, request, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("danger").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }

}
