package com.example.roadhazardmap.utils;

import android.graphics.Color;

import com.example.roadhazardmap.models.Hazard;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

public class HazardUtils {

    // Proximity threshold in metres — warn rider if within this distance of a hazard
    public static final double PROXIMITY_THRESHOLD_METRES = 100.0;

    /**
     * Returns the hex colour string for a given hazard class.
     *  pothole      → Red      #E53935
     *  speed_bump   → Orange   #FB8C00
     *  crosswalk    → Blue     #1E88E5
     *  road_debris  → Yellow   #FDD835
     */
    public static int getColorForClass(String hazardClass) {
        if (hazardClass == null) return Color.GRAY;
        switch (hazardClass) {
            case Hazard.CLASS_POTHOLE:      return Color.parseColor("#E53935");
            case Hazard.CLASS_SPEED_BUMP:   return Color.parseColor("#FB8C00");
            case Hazard.CLASS_CROSSWALK:    return Color.parseColor("#1E88E5");
            case Hazard.CLASS_ROAD_DEBRIS:  return Color.parseColor("#FDD835");
            default:                        return Color.GRAY;
        }
    }

    /**
     * Returns a BitmapDescriptorFactory hue for Google Maps marker.
     */
    public static BitmapDescriptor getMarkerIconForClass(String hazardClass) {
        float hue;
        if (hazardClass == null) {
            hue = BitmapDescriptorFactory.HUE_VIOLET;
        } else {
            switch (hazardClass) {
                case Hazard.CLASS_POTHOLE:
                    hue = BitmapDescriptorFactory.HUE_RED;
                    break;
                case Hazard.CLASS_SPEED_BUMP:
                    hue = BitmapDescriptorFactory.HUE_ORANGE;
                    break;
                case Hazard.CLASS_CROSSWALK:
                    hue = BitmapDescriptorFactory.HUE_AZURE;
                    break;
                case Hazard.CLASS_ROAD_DEBRIS:
                    hue = BitmapDescriptorFactory.HUE_YELLOW;
                    break;
                default:
                    hue = BitmapDescriptorFactory.HUE_VIOLET;
            }
        }
        return BitmapDescriptorFactory.defaultMarker(hue);
    }

    /**
     * Returns the emoji icon for a hazard class (used in alerts / snippet).
     */
    public static String getEmojiForClass(String hazardClass) {
        if (hazardClass == null) return "⚠️";
        switch (hazardClass) {
            case Hazard.CLASS_POTHOLE:      return "🕳️";
            case Hazard.CLASS_SPEED_BUMP:   return "🚧";
            case Hazard.CLASS_CROSSWALK:    return "🦓";
            case Hazard.CLASS_ROAD_DEBRIS:  return "🪨";
            default:                        return "⚠️";
        }
    }

    /**
     * Haversine formula — returns distance in metres between two lat/lng points.
     */
    public static double distanceMetres(LatLng a, LatLng b) {
        final int R = 6371000; // Earth radius in metres
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLng = Math.toRadians(b.longitude - a.longitude);

        double sinDLat = Math.sin(dLat / 2);
        double sinDLng = Math.sin(dLng / 2);
        double x = sinDLat * sinDLat
                + Math.cos(lat1) * Math.cos(lat2) * sinDLng * sinDLng;
        double c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
        return R * c;
    }

    /**
     * Returns the snippet text shown beneath the marker title on the map.
     */
    public static String getSnippetForHazard(Hazard h) {
        return String.format("%s  Lat: %.5f  Lng: %.5f  (%.0f%% conf.)",
                getEmojiForClass(h.getHazardClass()),
                h.getLatitude(), h.getLongitude(),
                h.getConfidence() * 100);
    }
}
