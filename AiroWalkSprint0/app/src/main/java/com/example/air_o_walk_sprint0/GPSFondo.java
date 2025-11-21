package com.example.air_o_walk_sprint0;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.ActivityCompat;

public class GPSFondo implements LocationListener {

    private static final String TAG = "GPSFondo";

    // GPS settings - updates every 10 seconds
    private static final long MIN_TIME_BETWEEN_UPDATES = 10000; // 10 seconds
    private static final float MIN_DISTANCE_CHANGE = 0; // 0 meters (update based on time only)

    private Context context;
    private LocationManager locationManager;
    private Location currentLocation;
    private boolean isTracking = false;

    private LocationUpdateListener listener;

    public interface LocationUpdateListener {
        void onLocationUpdate(Location location);
        void onLocationError(String error);
    }

    public GPSFondo(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.listener = listener;
    }

    public void startTracking() {
        if (isTracking) {
            Log.d(TAG, "GPS tracking already active");
            return;
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "GPS permission not granted");
            if (listener != null) {
                listener.onLocationError("GPS permission not granted");
            }
            return;
        }

        // Request updates from GPS provider
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    this
            );
            isTracking = true;
            Log.d(TAG, "GPS tracking started - updates every 10 seconds");
        } else {
            Log.e(TAG, "GPS provider is not enabled");
            if (listener != null) {
                listener.onLocationError("GPS is disabled");
            }
        }

        // Also try Network provider as backup
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    this
            );
        }
    }

    public void stopTracking() {
        if (!isTracking) {
            return;
        }

        locationManager.removeUpdates(this);
        isTracking = false;
        Log.d(TAG, "GPS tracking stopped");
    }

    public void resetTracking() {
        currentLocation = null;
        Log.d(TAG, "GPS tracking reset");
    }

    @Override
    public void onLocationChanged(Location location) {
        // Filter out low accuracy locations
        if (location.getAccuracy() > 50) {
            Log.d(TAG, "Location ignored - poor accuracy: " + location.getAccuracy() + "m");
            return;
        }

        currentLocation = location;

        Log.d(TAG, String.format("New location: Lat=%.6f, Lon=%.6f, Accuracy=%.1fm",
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy()));

        if (listener != null) {
            listener.onLocationUpdate(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Provider status changed: " + provider + " - Status: " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Provider disabled: " + provider);
        if (listener != null) {
            listener.onLocationError("GPS provider disabled");
        }
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public boolean isTracking() {
        return isTracking;
    }

    public double getCurrentLatitude() {
        return currentLocation != null ? currentLocation.getLatitude() : 0.0;
    }

    public double getCurrentLongitude() {
        return currentLocation != null ? currentLocation.getLongitude() : 0.0;
    }

    public float getCurrentAccuracy() {
        return currentLocation != null ? currentLocation.getAccuracy() : 0.0f;
    }

    public String getCurrentLocationString() {
        if (currentLocation == null) {
            return "No location available";
        }
        return String.format("Lat: %.6f, Lon: %.6f (±%.1fm)",
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                currentLocation.getAccuracy());
    }
}
