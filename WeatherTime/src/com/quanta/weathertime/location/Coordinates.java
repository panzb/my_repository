package com.quanta.weathertime.location;

public class Coordinates {
    public double latitude;
    public double longitude;

    public static final Coordinates UNDEFINED = new Coordinates(0, 0);

    public Coordinates(double lat, double lon) {
        latitude = lat;
        longitude = lon;
    }

    public String toString() {
        return "Lat: " + latitude + ", Lon: " + longitude;
    }
}