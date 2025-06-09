package pednav.backend.pednav.service;

import org.json.JSONObject;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pednav.backend.pednav.repository.DataRepository;
import pednav.backend.pednav.websocket.Case2SensorBuffer;
import pednav.backend.pednav.websocket.Case3SensorBuffer;
import pednav.backend.pednav.websocket.Case4SensorBuffer;

@Service
public class SyncService {

    private final DataRepository repository;
    private final Case2SensorBuffer case2Buffer;
    private final Case3SensorBuffer case3Buffer;
    private final Case4SensorBuffer case4Buffer;


    public SyncService(
            DataRepository repository,
            @Lazy Case2SensorBuffer case2Buffer,
            @Lazy Case3SensorBuffer case3Buffer,
            @Lazy Case4SensorBuffer case4Buffer // ✅ 추가

    ) {
        this.repository = repository;
        this.case2Buffer = case2Buffer;
        this.case3Buffer = case3Buffer;
        this.case4Buffer = case4Buffer;
    }

    public void processIncomingJson(String json) {
        try {
            System.out.println("📥 수신된 JSON: " + json);
            JSONObject obj = new JSONObject(json);
            long timestamp = obj.getLong("timestamp");

            // ✅ vehicle_detected: Case2
            if (obj.has("vehicle_detected")) {
                float val = (float) obj.getDouble("vehicle_detected");
                case2Buffer.putRadarData(timestamp, val);
                case3Buffer.putAndroidData(timestamp, val); // Case3에도 사용됨
            }

            if (obj.has("velocity") && obj.has("distance")) {
                float vel = obj.getFloat("velocity");
                float dist = obj.getFloat("distance");

                case3Buffer.putESP32Data(timestamp, vel, dist);
                case4Buffer.putVelocityDistance(timestamp, vel, dist);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
