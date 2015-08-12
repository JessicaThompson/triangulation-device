package ca.triangulationdevice.android.model;

import android.graphics.drawable.Drawable;
import android.location.Location;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.UUID;

public class User {
    public String id;
    public String name = "";
    public String email = "";
    public String description = "";
    public String location = "";
    public Location myLocation;
    public Drawable picture;
    public boolean online = false;

    public User(String id) {
        this.id = id;
    }
}
