package ca.triangulationdevice.android.storage;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;
import ca.triangulationdevice.android.util.Installation;

//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonSerializationContext;
//import com.google.gson.JsonSerializer;

/**
 * Stores and provides access to two "paths" (mine and theirs), represented by
 * lists of {@link Location}s. Uses the SQLite database internally to hold our
 * values.
 */
public class PathStorage {
    private static final String TAG = "PathStorage";

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
    private final String[] locationColumns = { PathSQLiteHelper.COLUMN_LOCATION_ID, PathSQLiteHelper.COLUMN_LOCATION_PATH_ID, PathSQLiteHelper.COLUMN_LOCATION_LATITUDE, PathSQLiteHelper.COLUMN_LOCATION_LONGITUDE, PathSQLiteHelper.COLUMN_LOCATION_AZIMUTH, PathSQLiteHelper.COLUMN_LOCATION_PITCH, PathSQLiteHelper.COLUMN_LOCATION_ROLL, PathSQLiteHelper.COLUMN_LOCATION_TIME };

    private final List<List<Point>> paths = new ArrayList<List<Point>>(PATH_TOTAL);
    private String uid;
//    private final Gson gson;

    private Context context;

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
//        this.context = context;
//        this.paths.add(MINE, new ArrayList<Point>());
//        this.paths.add(THEIRS, new ArrayList<Point>());
//
        this.pathHelper = new PathSQLiteHelper(context);
//
//        // Create the GSON object with the type adapter.
//        GsonBuilder gsonBuilder = new GsonBuilder();
//        gsonBuilder.registerTypeAdapter(Point.class, new JsonSerializer<Point>() {
//            @Override
//            public JsonElement serialize(Point point, Type type, JsonSerializationContext context) {
//                JsonObject object = new JsonObject();
//                object.addProperty("id", point.id);
//                object.addProperty("latitude", point.location.getLatitude());
//                object.addProperty("longitude", point.location.getLongitude());
//                object.addProperty("azimuth", point.azimuth);
//                object.addProperty("pitch", point.pitch);
//                object.addProperty("roll", point.roll);
//                object.addProperty("time", point.location.getTime());
//                return object;
//            }
//        });
//        this.gson = gsonBuilder.create();
    }

    /**
     * Open our connection to the underlying storage system.
     * 
     * @throws SQLException
     *             if no database is available.
     */
    public void open() throws SQLException {
        this.database = this.pathHelper.getWritableDatabase();
        this.uid = Installation.id(this.context);
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
    public void addMine(Location point, float azimuth, float pitch, float roll) {
        this.paths.get(MINE).add(new Point(point, azimuth, pitch, roll));
    }

    /**
     * Add a new location to their path.
     * 
     * @param point
     *            A location to use for their path.
     */
    public void addTheirs(Location point) {
        this.paths.get(THEIRS).add(new Point(point, 0, 0, 0));
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
     * Don't use this on the UI thread. Use the LoadSessionsTask for that.
     * 
     * @return A list of sessions.
     */
    private List<Session> loadSessions() {
        // First, we load all the paths.
        SparseArray<List<Path>> pathArray = new SparseArray<List<Path>>();
        Cursor pathCursor = this.database.query(PathSQLiteHelper.TABLE_PATHS, this.pathColumns, null, null, null, null, null);
        pathCursor.moveToFirst();

        while (!pathCursor.isAfterLast()) {
            Path path = new Path(pathCursor);

            // If it's not in our map, add an array to hold them.
            int sessionId = pathCursor.getInt(pathCursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_SESSION_ID));
            if (pathArray.indexOfKey(sessionId) < 0) {
                List<Path> pathList = new ArrayList<Path>(PATH_TOTAL);
                pathArray.put(sessionId, pathList);
            }

            // Add it to our map.
            int type = pathCursor.getInt(pathCursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_TYPE));
            if (type == MINE && pathArray.get(sessionId).size() == 1) {
                pathArray.get(sessionId).add(0, path);
            } else {
                pathArray.get(sessionId).add(path);
            }
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
                System.out.println(session.id);
                if (pathArray.indexOfKey(session.id) > 0) {
                    session.paths = pathArray.get(session.id);
                } else {
                    session.paths = Collections.emptyList();
                    Log.w(TAG, String.format("No paths found for \"\" session.", session.title));
                }
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
     * @throws ParseException
     */
    private List<Point> loadPoints(Path path) throws ParseException {
        Cursor cursor = this.database.query(PathSQLiteHelper.TABLE_LOCATIONS, this.locationColumns, null, null, null, null, null);
        cursor.moveToFirst();
        List<Point> locations = new ArrayList<Point>();

        while (!cursor.isAfterLast()) {
            locations.add(new Point(cursor));
            cursor.moveToNext();
        }

        cursor.close();
        path.points = locations;
        return locations;
    }

    private List<Session> loadSessionsAndPoints() throws ParseException {
        List<Session> sessions = this.loadSessions();
        for (Session session : sessions) {
            for (Path path : session.paths) {
                path.points = this.loadPoints(path);
            }
        }

        return sessions;
    }

    // Saves a session with the current title, returns the ID.
    private long saveSession(String title) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(PathSQLiteHelper.COLUMN_SESSION_TITLE, title);
        sessionValues.put(PathSQLiteHelper.COLUMN_SESSION_TIME_SAVED, df.format(new Date()));
        long id = this.database.insert(PathSQLiteHelper.TABLE_SESSIONS, null, sessionValues);
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

    // Save a point to the database, linked to the given path, returns the ID.
    private long savePoint(Point point, long pathId) {
        ContentValues locationValues = new ContentValues();
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_PATH_ID, pathId);
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_LATITUDE, point.location.getLatitude());
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_LONGITUDE, point.location.getLongitude());
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_AZIMUTH, point.azimuth);
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_PITCH, point.azimuth);
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_ROLL, point.roll);
        locationValues.put(PathSQLiteHelper.COLUMN_LOCATION_TIME, df.format(new Date(point.location.getTime())));
        return this.database.insert(PathSQLiteHelper.TABLE_LOCATIONS, null, locationValues);
    }

    private void deleteAll() {
        this.database.delete(PathSQLiteHelper.TABLE_LOCATIONS, null, null);
        this.database.delete(PathSQLiteHelper.TABLE_PATHS, null, null);
        this.database.delete(PathSQLiteHelper.TABLE_SESSIONS, null, null);
    }

    private void deleteSession(long sessionId) {
        // We need to get all of the paths in the session.
        Cursor pathCursor = this.database.query(PathSQLiteHelper.TABLE_PATHS, this.pathColumns, "session_id=?", new String[] { Long.toString(sessionId) }, null, null, null);
        pathCursor.moveToFirst();
        while (!pathCursor.isAfterLast()) {
            long pathId = pathCursor.getLong(pathCursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_ID));
            this.database.delete(PathSQLiteHelper.TABLE_PATHS, PathSQLiteHelper.COLUMN_PATH_ID + "=?", new String[] { Long.toString(pathId) });
            this.database.delete(PathSQLiteHelper.TABLE_LOCATIONS, PathSQLiteHelper.COLUMN_LOCATION_PATH_ID + "=?", new String[] { Long.toString(pathId) });
            pathCursor.moveToNext();
        }

        // Now that the locations and paths are gone, delete the session.
        this.database.delete(PathSQLiteHelper.TABLE_SESSIONS, PathSQLiteHelper.COLUMN_SESSION_ID + "=?", new String[] { Long.toString(sessionId) });
    }

    /**
     * This task deletes all sessions. Use it wisely.
     */
    public abstract class DeleteSessionsTask extends AsyncTask<Long, Void, Void> {
        @Override
        protected Void doInBackground(Long... nothing) {
            PathStorage.this.deleteAll();
            return null;
        }

        @Override
        protected abstract void onPostExecute(Void result);
    }

    /**
     * This task deletes a specific session.
     */
    public abstract class DeleteOneSessionTask extends AsyncTask<Long, Void, Void> {
        @Override
        protected Void doInBackground(Long... ids) {
            for (long id : ids) {
                PathStorage.this.deleteSession(id);
            }
            return null;
        }

        @Override
        protected abstract void onPostExecute(Void result);
    }

    /**
     * This task loads the sessions...that's pretty much it. Returns a list of
     * sessions to onPostExecute, which users can anonymously-subclass and
     * implement.
     */
    public abstract class LoadSessionsTask extends AsyncTask<Void, List<Session>, List<Session>> {
        @Override
        protected List<Session> doInBackground(Void... nothing) {
            return PathStorage.this.loadSessions();
        }

        @Override
        protected abstract void onPostExecute(List<Session> result);
    }

    /**
     * This task saves the currently-loaded session, returning nothing to the
     * subclass. Subclasses can suck it, and/or implement a callback which
     * doesn't really need anything from you, DAD.
     */
    public abstract class SaveSessionTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... titles) {
            for (String title : titles) {
                long sessionId = saveSession(title);
                for (int i = 0; i < PathStorage.this.paths.size(); i++) {
                    long pathId = savePath(i, sessionId);
                    for (int j = 0; j < PathStorage.this.paths.get(i).size(); j++) {
                        savePoint(PathStorage.this.paths.get(i).get(j), pathId);
                    }
                }

                // Clear the paths now that we're done.
                for (int i = 0; i < PathStorage.this.paths.size(); i++) {
                    PathStorage.this.paths.set(i, new ArrayList<Point>());
                }
            }

            return null;
        }

        @Override
        protected abstract void onPostExecute(Void result);
    }

    /**
     * Sends the current sessions, paths, and individual points to the server.
     * It's pretty heavy on the database, so give it a minute.
     */
    public abstract class SendToServerTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... nothing) {
            // Load our sessions.
            try {
                List<Session> sessions = PathStorage.this.loadSessionsAndPoints();

                // TODO: Instead of saving to JSON, send to the server.
                // String filename = "sessions.json";
                // FileOutputStream outputStream =
                // PathStorage.this.context.openFileOutput(filename,
                // Context.MODE_PRIVATE);
                // outputStream.write(PathStorage.this.gson.toJson(sessions).getBytes());
                // outputStream.close();
            } catch (ParseException exception) {
                Toast.makeText(PathStorage.this.context, "Couldn't parse dates from database, error in application.", Toast.LENGTH_LONG).show();
                // } catch (FileNotFoundException e) {
                // Log.e(TAG,
                // "Couldn't find sessions.json file to write...except we're making it...I don't know why we'd hit this.");
                // } catch (IOException e) {
                // Log.e(TAG, "I/O exception writing files to sessions.json.");
            }

            return null;
        }

        @Override
        protected abstract void onPostExecute(Void result);
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
        public List<Point> points;

        public Path(Cursor cursor) {
            this.id = cursor.getInt(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_ID));
            this.type = cursor.getInt(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_PATH_TYPE));
        }
    }

    public class Point {
        public int id;
        public final Location location;
        public float azimuth;
        public float pitch;
        public float roll;

        public Point(Cursor cursor) throws ParseException {
            this.id = cursor.getInt(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_ID));
            this.location = buildLocation(cursor);
            this.azimuth = cursor.getFloat(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_AZIMUTH));
            this.pitch = cursor.getFloat(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_PITCH));
            this.roll = cursor.getFloat(cursor.getColumnIndex(PathSQLiteHelper.COLUMN_LOCATION_ROLL));
        }

        public Point(Location location, float azimuth, float pitch, float roll) {
            this.location = location;
            this.azimuth = azimuth;
            this.pitch = pitch;
            this.roll = roll;
        }
    }
}
