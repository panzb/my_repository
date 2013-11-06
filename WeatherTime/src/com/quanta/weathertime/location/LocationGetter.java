package com.quanta.weathertime.location;

import com.quanta.weathertime.location.LocationResolver.LocationResult;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

public class LocationGetter {
    private static final String TAG = "Weather.LocationGetter";

    private final Context context;
    private Location location = null;
    private Coordinates coordinates = null;
    private final Object gotLocationLock = new Object();
    private final LocationResult locationResult = new LocationResult() {
        @Override
        public void gotLocation(Location location) {
            synchronized (gotLocationLock) {
                LocationGetter.this.location = location;
                gotLocationLock.notifyAll();
                Looper.myLooper().quit();
            }
        }
    };

    public LocationGetter(Context context) {
        if (context == null)
            throw new IllegalArgumentException("context == null");

        this.context = context;
    }

    public synchronized Coordinates getLocation(int maxWaitingTime, int updateTimeout) {
        try {
            final int updateTimeoutPar = updateTimeout;
            synchronized (gotLocationLock) {
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        LocationResolver locationResolver = new LocationResolver();
                        locationResolver.prepare();
                        locationResolver.getLocation(context, locationResult, updateTimeoutPar);
                        Looper.loop();
                    }
                }.start();

                gotLocationLock.wait(maxWaitingTime);
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        if (location != null) {
            coordinates = new Coordinates(location.getLatitude(), location.getLongitude());
        }
        else {
            coordinates = Coordinates.UNDEFINED;
        }

        Log.i(TAG, "#getLocation: coordinates=" + coordinates);

        return coordinates;
    }
}
