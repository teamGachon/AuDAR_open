package pednav.backend.pednav.dto;

import pednav.backend.pednav.domain.entity.DetectionResult;


public class Case3DangerRequest {
    private Long timestamp;
    private Double soundDetected;
    private Double velocity, distance;
    private String danger;
    public Double getSoundDetected() {return soundDetected;}
    public Long getTimestamp() { return timestamp; }
    public Double getVelocity() { return velocity; }
    public Double getDistance() { return distance; }
    public String getDanger() { return danger; }


    public Case3DangerRequest() {}


    public Case3DangerRequest(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSoundDetected(Double val) { this.soundDetected = val; }
    public void setVelocity(Double v) { this.velocity = v; }
    public void setDistance(Double d) { this.distance = d; }
    public void setDanger(String danger) { this.danger = danger; }


    public boolean isComplete() {
        return soundDetected != null && velocity != null && distance != null;
    }

    public DetectionResult toEntity() {
        DetectionResult entity = new DetectionResult();
        entity.setTimestamp(timestamp);
        entity.setSoundDetected(soundDetected);
        entity.setVelocity(velocity);
        entity.setDistance(distance);
        entity.setDanger(danger);
        return entity;
    }
}

