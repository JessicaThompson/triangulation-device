package ca.triangulationdevice.android.model;

import android.graphics.drawable.Drawable;
import android.location.Location;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.UUID;

public class User {
    public final String id;
    public String name;
    public String email;
    public String description;
    public String location;
    public Location myLocation;
    public Drawable picture;
    public boolean online = true;

    public User(String name, String description, String location, Drawable picture) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.location = location;
        this.picture = picture;
    }

    public User(String name, String description, String location) {
        this(name, description, location, null);
    }
}
