package ca.triangulationdevice.android.model;

import android.location.Location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ca.triangulationdevice.android.model.serialize.LocationDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Session extends CouchObject {
    public String audioFilename;
    public String ownerId;
    public String title;
    public String description;
    public Date saved;
    @JsonDeserialize(using = LocationDeserializer.class)
    public Location startLocation;
    public String location;
    public List<Path> paths = new ArrayList<>(2);

    {
        this.paths.add(Path.MINE, new Path());
        this.paths.add(Path.THEIRS, new Path());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path extends CouchObject {
        public final static int MINE = 0;
        public final static int THEIRS = 1;
        public List<Point> points = new ArrayList<>();

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Point {
            @JsonDeserialize(using = LocationDeserializer.class)
            public Location location;
            public float azimuth;
            public float pitch;
            public float roll;
        }

        public void addPoint(Location location, float azimuth, float pitch, float roll) {
            Point point = new Point();
            point.location = location;
            point.azimuth = azimuth;
            point.pitch = pitch;
            point.roll = roll;
            this.points.add(point);
        }
    }
}
