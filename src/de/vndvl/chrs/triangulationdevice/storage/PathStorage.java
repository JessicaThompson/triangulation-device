package de.vndvl.chrs.triangulationdevice.storage;

import java.util.ArrayList;
import java.util.Date;

import android.location.Location;
import android.util.Pair;

public class PathStorage {
    private ArrayList<Pair<Location, Date>> path = new ArrayList<Pair<Location, Date>>();
    
    public void add(Location point) {
        this.path.add(new Pair<Location, Date>(point, new Date()));
    }
    
    public ArrayList<Pair<Location, Date>> end() {
        ArrayList<Pair<Location, Date>> finishedPath = this.path;
        this.path = new ArrayList<Pair<Location, Date>>();
        return finishedPath;
    }
}
