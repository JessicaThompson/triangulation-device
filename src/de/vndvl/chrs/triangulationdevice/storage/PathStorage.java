package de.vndvl.chrs.triangulationdevice.storage;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

public class PathStorage {
    
    // Storage locations in the list, extendable juuuuuust in case.
    private static final int MINE = 0;
    private static final int THEIRS = 1;
    
    private static final int PATH_TOTAL = 2; // Number of total paths (mine + theirs = 2 for now);
    
    // Database fields
    private SQLiteDatabase database;
    private PathSQLiteHelper pathHelper;
    
    private static TimeZone tz = TimeZone.getTimeZone("UTC");
    private static DateFormat df = SimpleDateFormat.getDateInstance();
    
    private String[] sessionColumns = { PathSQLiteHelper.COLUMN_SESSION_ID, PathSQLiteHelper.COLUMN_SESSION_TITLE, PathSQLiteHelper.COLUMN_SESSION_TIME_SAVED };
    private String[] pathColumns = { PathSQLiteHelper.COLUMN_PATH_ID, PathSQLiteHelper.COLUMN_PATH_SESSION_ID, PathSQLiteHelper.COLUMN_PATH_TYPE };
    private String[] locationColumns = { PathSQLiteHelper.COLUMN_LOCATION_ID, PathSQLiteHelper.COLUMN_LOCATION_PATH_ID, PathSQLiteHelper.COLUMN_LOCATION_LATITUDE, PathSQLiteHelper.COLUMN_LOCATION_LONGITUDE, PathSQLiteHelper.COLUMN_LOCATION_TIME };

    private List<List<Location>> paths = new ArrayList<List<Location>>(PATH_TOTAL);
    
    // Why hello there obscure Java features.
    static {
        df.setTimeZone(tz);
    }

    public PathStorage(Context context) {
        this.paths.add(MINE, new ArrayList<Location>());
        this.paths.add(THEIRS, new ArrayList<Location>());
        
        pathHelper = new PathSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = pathHelper.getWritableDatabase();
    }

    public void close() {
        pathHelper.close();
    }

    public void addMine(Location point) {
        this.paths.get(MINE).add(point);
    }
    
    public void addTheirs(Location point) {
        this.paths.get(THEIRS).add(point);
    }
    
    private static Location buildLocation(Cursor cursor) throws ParseException {
        Location location = new Location("saved");
        location.setLatitude(cursor.getDouble(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_LATITUDE)));
        location.setLatitude(cursor.getDouble(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_LONGITUDE)));
        Date time = df.parse(cursor.getString(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_TIME)));
        location.setTime(time.getTime());
        return location;
    }
    
    @SuppressLint("UseSparseArrays") // I think we need them...?
    public List<Session> loadSessions() {
        // First, we load all the paths.
        Map<Integer, List<Path>> pathMap = new HashMap<Integer, List<Path>>();
        Cursor pathCursor = database.query(PathSQLiteHelper.TABLE_PATHS, pathColumns, null, null, null, null, null);
        pathCursor.moveToFirst();
        
        while (!pathCursor.isAfterLast()) {
            Path path = new Path(pathCursor);
            
            // If it's not in our map, add an array to hold them.
            int sessionId = pathCursor.getInt(pathCursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_SESSION_ID));
            int type = pathCursor.getInt(pathCursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_TYPE));
            if (!pathMap.containsKey(sessionId)) {
                pathMap.put(sessionId, new ArrayList<Path>(PATH_TOTAL));
            }
            
            // Add it to our map.
            pathMap.get(sessionId).add(type, path);
            pathCursor.moveToNext();
        }
        pathCursor.close();
        
        // Now load all the sessions, adding the paths we have
        Cursor sessionCursor = database.query(PathSQLiteHelper.TABLE_SESSIONS, sessionColumns, null, null, null, null, null);
        sessionCursor.moveToFirst();
        List<Session> sessions = new ArrayList<Session>();
        
        while (!sessionCursor.isAfterLast()) {
            try {
                Session session = new Session(sessionCursor);
                session.paths = pathMap.get(session.id);
                sessions.add(session);
                sessionCursor.moveToNext();
            } catch (ParseException exception) {
                Log.e(PathStorage.class.getName(), "Exception parsing the save date from the database on a path.", exception);
            }
        }
        
        sessionCursor.close();
        return sessions;
    }

    public List<Location> loadPoints(Path path) {
        Cursor cursor = database.query(PathSQLiteHelper.TABLE_LOCATIONS, locationColumns, null, null, null, null, null);
        cursor.moveToFirst();
        List<Location> locations = new ArrayList<Location>();
        
        while (!cursor.isAfterLast()) {
            try {
                locations.add(buildLocation(cursor));
                cursor.moveToNext();
            } catch (ParseException exception) {
                Log.e(PathStorage.class.getName(), "Error parsing location time from the database.", exception);
            }
        }
        
        cursor.close();
        return locations;
    }
    
    private long saveSession(String title) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(PathSQLiteHelper.COLUMN_SESSION_TITLE, title);
        sessionValues.put(PathSQLiteHelper.COLUMN_SESSION_TIME_SAVED, df.format(new Date()));
        return database.insert(PathSQLiteHelper.TABLE_SESSIONS, null, sessionValues);
    }
    
    private long savePath(int type, long sessionId) {
        ContentValues pathValues = new ContentValues();
        pathValues.put(PathSQLiteHelper.COLUMN_SESSION_ID, sessionId);
        pathValues.put(PathSQLiteHelper.COLUMN_PATH_TYPE, type);
        return database.insert(PathSQLiteHelper.TABLE_PATHS, null, pathValues);
    }
    
    private long saveLocation(Location point, long pathId) {
        ContentValues locationValues = new ContentValues();
        locationValues.put(PathSQLiteHelper.COLUMN_PATH_ID, pathId);
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_LATITUDE, point.getLatitude());
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_LONGITUDE, point.getLongitude());
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_TIME, df.format(new Date(point.getTime())));
        return database.insert(PathSQLiteHelper.TABLE_LOCATIONS, null, locationValues);
    }
    
    public void finish(String title) {
        long sessionId = saveSession(title);
        for (int i = 0; i < this.paths.size(); i++) {
            long pathId = savePath(i, sessionId);
            for (Location point : this.paths.get(i)) {
                saveLocation(point, pathId);
            }
        }
        
        // TODO: Send the data online somewhere?
        
        // Clear the paths now that we're done.
        for (int i = 0; i < this.paths.size(); i++) {
            this.paths.set(i, new ArrayList<Location>());
        }
    }
    
    public class Session {
        public final int id;
        public final String title;
        public final Date saved;
        public List<Path> paths;
        
        public Session(Cursor cursor) throws ParseException {
            this.id = cursor.getInt(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_ID));
            this.title = cursor.getString(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_SESSION_TITLE));
            this.saved = df.parse(cursor.getString(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_SESSION_TIME_SAVED)));
        }
    }
    
    public class Path {
        public final int id;
        public final int type;
        
        public Path(Cursor cursor) {
            this.id = cursor.getInt(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_ID));
            this.type = cursor.getInt(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_TYPE));
        }
    }
}
