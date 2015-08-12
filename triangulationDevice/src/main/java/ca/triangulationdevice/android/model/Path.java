package ca.triangulationdevice.android.model;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

public class Path {
    public final static int MINE = 0;
    public final static int THEIRS = 1;
    public List<Point> points = new ArrayList<>();

    public static class Point {
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
