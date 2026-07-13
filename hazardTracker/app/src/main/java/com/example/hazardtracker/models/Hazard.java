package com.example.hazardtracker.models;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.Timestamp;

public class Hazard {

    private String userName;
    private String category;       // e.g. "Road Hazard", "Environmental Hazard", "Building Hazard"
    private String description;
    private String deviceModel;
    private GeoPoint location;
    private String locationName;
    private Timestamp reportedAt;
    private String status;

    // Required empty constructor for Firestore
    public Hazard() {
    }

    public String getUserName() {
        return userName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public String getLocationName() {
        return locationName;
    }

    public Timestamp getReportedAt() {
        return reportedAt;
    }

    public String getStatus() {
        return status;
    }
}