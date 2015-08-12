package ca.triangulationdevice.android.model;

import java.util.Date;
import java.util.List;

public class Session {
    public String id;
    public String ownerId;
    public String title;
    public Date saved;
    public String locationString;
    public List<Path> paths;
}
