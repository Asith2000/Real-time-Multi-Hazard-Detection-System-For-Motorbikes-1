package com.example.roadhazardmap.models;

public class Hazard {

    // Hazard class constants
    public static final String CLASS_POTHOLE      = "pothole";
    public static final String CLASS_SPEED_BUMP   = "speed_bump";
    public static final String CLASS_CROSSWALK    = "crosswalk";
    public static final String CLASS_ROAD_DEBRIS  = "road_debris";

    private String id;
    private String hazardClass;
    private double latitude;
    private double longitude;
    private long timestamp;
    private float confidence;
    private String deviceId;       // Raspberry Pi device ID
    private String imageUrl;       // Optional snapshot from Pi camera

    // Required empty constructor for Firebase
    public Hazard() {}

    public Hazard(String id, String hazardClass, double latitude, double longitude,
                  long timestamp, float confidence, String deviceId) {
        this.id = id;
        this.hazardClass = hazardClass;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.confidence = confidence;
        this.deviceId = deviceId;
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHazardClass() { return hazardClass; }
    public void setHazardClass(String hazardClass) { this.hazardClass = hazardClass; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /**
     * Returns a human-readable label for the hazard class.
     */
    public String getLabel() {
        if (hazardClass == null) return "Unknown";
        switch (hazardClass) {
            case CLASS_POTHOLE:     return "Pothole";
            case CLASS_SPEED_BUMP:  return "Speed Bump";
            case CLASS_CROSSWALK:   return "Crosswalk";
            case CLASS_ROAD_DEBRIS: return "Road Debris";
            default:                return hazardClass;
        }
    }
}
