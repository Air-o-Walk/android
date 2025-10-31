package com.example.air_o_walk_sprint0;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.TimeUnit;

public class WalkingTimeTracker {

    private long startTime = 0;
    private long totalElapsedTime = 0; // Total time in milliseconds
    private long pausedTime = 0;

    private boolean isTracking = false;
    private boolean isPaused = false;

    private Handler handler;
    private Runnable updateRunnable;

    private TimeUpdateListener listener;

    // Interface for real-time updates
    public interface TimeUpdateListener {
        void onTimeUpdate(long hours, long minutes, long seconds, long totalSeconds);
    }

    public WalkingTimeTracker() {
        handler = new Handler(Looper.getMainLooper());
    }

    // Start tracking time
    public void startTracking() {
        if (!isTracking) {
            startTime = System.currentTimeMillis();
            isTracking = true;
            isPaused = false;
            startUpdating();
        } else if (isPaused) {
            resume();
        }
    }

    // Pause tracking (keeps accumulated time)
    public void pause() {
        if (isTracking && !isPaused) {
            isPaused = true;
            pausedTime = System.currentTimeMillis();
            totalElapsedTime += (pausedTime - startTime);
            stopUpdating();
        }
    }

    // Resume tracking
    public void resume() {
        if (isTracking && isPaused) {
            isPaused = false;
            startTime = System.currentTimeMillis();
            startUpdating();
        }
    }

    // Stop tracking completely
    public void stopTracking() {
        if (isTracking) {
            if (!isPaused) {
                totalElapsedTime += (System.currentTimeMillis() - startTime);
            }
            isTracking = false;
            isPaused = false;
            stopUpdating();
        }
    }

    // Reset all tracking data
    public void reset() {
        stopTracking();
        startTime = 0;
        totalElapsedTime = 0;
        pausedTime = 0;

        if (listener != null) {
            listener.onTimeUpdate(0, 0, 0, 0);
        }
    }

    // Get current elapsed time in milliseconds
    public long getElapsedTimeMillis() {
        if (!isTracking) {
            return totalElapsedTime;
        }

        if (isPaused) {
            return totalElapsedTime;
        }

        return totalElapsedTime + (System.currentTimeMillis() - startTime);
    }

    // Get elapsed time in seconds
    public long getElapsedTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getElapsedTimeMillis());
    }

    // Get elapsed time in minutes
    public long getElapsedTimeMinutes() {
        return TimeUnit.MILLISECONDS.toMinutes(getElapsedTimeMillis());
    }

    // Get elapsed time in hours
    public long getElapsedTimeHours() {
        return TimeUnit.MILLISECONDS.toHours(getElapsedTimeMillis());
    }

    // Get formatted time string (HH:MM:SS)
    public String getFormattedTime() {
        long millis = getElapsedTimeMillis();

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Get formatted time string (MM:SS) - for shorter displays
    public String getFormattedTimeShort() {
        long millis = getElapsedTimeMillis();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    // Get time components separately
    public TimeComponents getTimeComponents() {
        long millis = getElapsedTimeMillis();

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return new TimeComponents(hours, minutes, seconds, totalSeconds);
    }

    // Set listener for real-time updates
    public void setTimeUpdateListener(TimeUpdateListener listener) {
        this.listener = listener;
    }

    // Start real-time updates (every second)
    private void startUpdating() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking && !isPaused && listener != null) {
                    TimeComponents time = getTimeComponents();
                    listener.onTimeUpdate(time.hours, time.minutes,
                            time.seconds, time.totalSeconds);
                }
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(updateRunnable);
    }

    private void stopUpdating() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    public boolean isTracking() {
        return isTracking;
    }

    public boolean isPaused() {
        return isPaused;
    }

    // Clean up resources
    public void destroy() {
        stopUpdating();
        handler.removeCallbacksAndMessages(null);
    }

    // Data class for time components
    public static class TimeComponents {
        public long hours;
        public long minutes;
        public long seconds;
        public long totalSeconds;

        public TimeComponents(long h, long m, long s, long total) {
            this.hours = h;
            this.minutes = m;
            this.seconds = s;
            this.totalSeconds = total;
        }
    }

    // Calculate average pace (minutes per kilometer)
    public double calculatePaceMinPerKm(double distanceMeters) {
        if (distanceMeters == 0) return 0;

        long totalMinutes = getElapsedTimeMinutes();
        double distanceKm = distanceMeters / 1000.0;

        return totalMinutes / distanceKm;
    }

    // Calculate average speed (km/h)
    public double calculateSpeedKmPerHour(double distanceMeters) {
        long totalSeconds = getElapsedTimeSeconds();
        if (totalSeconds == 0) return 0;

        double distanceKm = distanceMeters / 1000.0;
        double hours = totalSeconds / 3600.0;

        return distanceKm / hours;
    }
}