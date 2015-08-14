package ca.triangulationdevice.android.model;

import android.graphics.drawable.Drawable;
import android.location.Location;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ca.triangulationdevice.android.model.serialize.LocationDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends CouchObject {
    public String name = "";
    public String email = "";
    public String description = "";
    public String location = "";
    public String ip = "";
    @JsonDeserialize(using = LocationDeserializer.class)
    public Location myLocation;
    @JsonIgnore public Drawable picture;
    public boolean online = false;
}
