package ca.triangulationdevice.android.storage;

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

/**
 * Stores and provides access to two "paths" (mine and theirs), represented by
 * lists of {@link Location}s. Uses the SQLite database internally to hold our
 * values.
 */
public class PathStorage {

    // Storage locations in the list, extendable juuuuuust in case.
    public static final int MINE = 0;
    public static final int THEIRS = 1;

    private static final int PATH_TOTAL = 2; // Number of total paths (mine +
                                             // theirs = 2 for now);

    // Database fields
    private SQLiteDatabase database;
    private final PathSQLiteHelper pathHelper;

    private static TimeZone tz = TimeZone.getTimeZone("UTC");
    private static DateFormat df = SimpleDateFormat.getDateInstance();

    private final String[] sessionColumns = { PathSQLiteHelper.COLUMN_SESSION_ID, PathSQLiteHelper.COLUMN_SESSION_TITLE, PathSQLiteHelper.COLUMN_SESSION_TIME_SAVED };
    private final String[] pathColumns = { PathSQLiteHelper.COLUMN_PATH_ID, PathSQLiteHelper.COLUMN_PATH_SESSION_ID, PathSQLiteHelper.COLUMN_PATH_TYPE };
    private final String[] locationColumns = { PathSQLiteHelper.COLUMN_LOCATION_ID, PathSQLiteHelper.COLUMN_LOCATION_PATH_ID, PathSQLiteHelper.COLUMN_LOCATION_LATITUDE, PathSQLiteHelper.COLUMN_LOCATION_LONGITUDE, PathSQLiteHelper.COLUMN_LOCATION_TIME };

    private final List<List<Location>> paths = new ArrayList<List<Location>>(PATH_TOTAL);

    // Why hello there obscure Java features.
    static {
        df.setTimeZone(tz);
    }

    /**
     * A constructor. What do you want from me?
     * 
     * @param context
     *            Either an Activity or something else which we'll use to get
     *            access to the databases.
     */
    public PathStorage(Context context) {
        this.paths.add(MINE, new ArrayList<Location>());
        this.paths.add(THEIRS, new ArrayList<Location>());

        this.pathHelper = new PathSQLiteHelper(context);
    }

    /**
     * Open our connection to the underlying storage system.
     * 
     * @throws SQLException
     *             if no database is available.
     */
    public void open() throws SQLException {
        this.database = this.pathHelper.getWritableDatabase();
    }

    /**
     * Close our connection to the underlying storage system.
     */
    public void close() {
        this.pathHelper.close();
    }

    /**
     * Add a new location to my path.
     * 
     * @param point
     *            A location to use for my path.
     */
    public void addMine(Location point) {
        this.paths.get(MINE).add(point);
    }

    /**
     * Add a new location to their path.
     * 
     * @param point
     *            A location to use for their path.
     */
    public void addTheirs(Location point) {
        this.paths.get(THEIRS).add(point);
    }

    // Convenience method to build a Location from a database cursor.
    private static Location buildLocation(Cursor cursor) throws ParseException {
        Location location = new Location("saved");
        location.setLatitude(cursor.getDouble(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_LATITUDE)));
        location.setLatitude(cursor.getDouble(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_LONGITUDE)));
        Date time = df.parse(cursor.getString(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_TIME)));
        location.setTime(time.getTime());
        return location;
    }

    /**
     * Loads and returns a list of {@link Session} objects for us to display or
     * load or something. Note that they don't include any of the underlying
     * location lists, which have to be loaded separately using
     * {@link #loadPoints(Path)}.
     * 
     * @return A list of sessions.
     */
    @SuppressLint("UseSparseArrays")
    // I think we need them...?
    public List<Session> loadSessions() {
        // First, we load all the paths.
        Map<Integer, List<Path>> pathMap = new HashMap<Integer, List<Path>>();
        Cursor pathCursor = this.database.query(PathSQLiteHelper.TABLE_PATHS, this.pathColumns, null, null, null, null, null);
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
        Cursor sessionCursor = this.database.query(PathSQLiteHelper.TABLE_SESSIONS, this.sessionColumns, null, null, null, null, null);
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

    /**
     * Load the raw list of points for a specific {@link Path}.
     * 
     * @param path
     *            The {@link Path} to get our points for, most likely contained
     *            within a session.
     * @return A list of {@link Location}s representing the path.
     */
    public List<Location> loadPoints(Path path) {
        Cursor cursor = this.database.query(PathSQLiteHelper.TABLE_LOCATIONS, this.locationColumns, null, null, null, null, null);
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
        path.points = locations;
        return locations;
    }

    // Saves a session with the current title, returns the ID.
    private long saveSession(String title) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(PathSQLiteHelper.COLUMN_SESSION_TITLE, title);
        sessionValues.put(PathSQLiteHelper.COLUMN_SESSION_TIME_SAVED, df.format(new Date()));
        long id = this.database.insert(PathSQLiteHelper.TABLE_SESSIONS, null, sessionValues);
        Log.i(getClass().toString(), "New ID: " + id);
        return id;
    }

    // Saves a path with the title specified, linked to the specific session ID,
    // returns the ID.
    private long savePath(int type, long sessionId) {
        ContentValues pathValues = new ContentValues();
        pathValues.put(PathSQLiteHelper.COLUMN_PATH_SESSION_ID, sessionId);
        pathValues.put(PathSQLiteHelper.COLUMN_PATH_TYPE, type);
        return this.database.insert(PathSQLiteHelper.TABLE_PATHS, null, pathValues);
    }

    // Save a location to the database, linked to the given path, returns the
    // ID.
    private long saveLocation(Location point, long pathId) {
        ContentValues locationValues = new ContentValues();
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_PATH_ID, pathId);
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_LATITUDE, point.getLatitude());
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_LONGITUDE, point.getLongitude());
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_TIME, df.format(new Date(point.getTime())));
        return this.database.insert(PathSQLiteHelper.TABLE_LOCATIONS, null, locationValues);
    }

    /**
     * Finish a session, saving it and its paths under the specific title and
     * resetting our temporary storage location.
     * 
     * @param title
     *            The title to save the session under.
     */
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

    /**
     * A POJO representing a session, containing its paths and some metadata.
     */
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

    /**
     * A POJO representing an individual path, containing its path and some
     * metadata. Pass it to {@link PathStorage#loadPoints(Path)} for the real
     * magic to happen.
     */
    public class Path {
        public final int id;
        public final int type;
        public List<Location> points;

        public Path(Cursor cursor) {
            this.id = cursor.getInt(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_ID));
            this.type = cursor.getInt(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_TYPE));
        }
    }
}
