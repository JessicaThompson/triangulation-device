package ca.triangulationdevice.android.model;

import android.graphics.drawable.Drawable;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.UUID;

public class User {
    public String id;
    public String name;
    public String description;
    public String location;
    public LatLng latLng;
    public Drawable picture;

    public User(String name, String description, String location, LatLng latLng, Drawable picture) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.location = location;
        this.picture = picture;
        this.latLng = latLng;
    }

    public User(String name, String description, String location, LatLng latLng) {
        this(name, description, location, latLng, null);
    }
}
