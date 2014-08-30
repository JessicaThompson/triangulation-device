package de.vndvl.chrs.triangulationdevice.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PathSQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "paths.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SESSIONS = "sessions";
    public static final String COLUMN_SESSION_ID = "id";
    public static final String COLUMN_SESSION_TITLE = "title";
    public static final String COLUMN_SESSION_TIME_SAVED = "time_saved";
    private static final String SESSIONS_CREATE = "create table " + TABLE_SESSIONS + "("
            + COLUMN_SESSION_ID + " integer primary key autoincrement, "
            + COLUMN_SESSION_TITLE + " text not null, "
            + COLUMN_SESSION_TIME_SAVED + " text not null"
    + ");";
    
    public static final String TABLE_PATHS = "paths";
    public static final String COLUMN_PATH_ID = "id";
    public static final String COLUMN_PATH_SESSION_ID = "session_id";
    public static final String COLUMN_PATH_TYPE = "type";
    private static final String PATHS_CREATE = "create table " + TABLE_PATHS + "("
            + COLUMN_PATH_ID + " integer primary key autoincrement, "
            + COLUMN_PATH_SESSION_ID + " integer not null, "
            + COLUMN_PATH_TYPE + " integer not null"
    + ");";
    
    public static final String TABLE_LOCATIONS = "location";
    public static final String COLUMN_LOCATION_ID = "id";
    public static final String COLUMN_LOCATION_PATH_ID = "path_id";
    public static final String COLUMN_LOCATION_LATITUDE = "latitude";
    public static final String COLUMN_LOCATION_LONGITUDE = "longitude";
    public static final String COLUMN_LOCATION_TIME = "time";
    private static final String LOCATIONS_CREATE = "create table " + TABLE_LOCATIONS + "("
            + COLUMN_LOCATION_ID + " integer primary key autoincrement, "
            + COLUMN_LOCATION_PATH_ID + " text not null,"
            + COLUMN_LOCATION_LATITUDE + " real not null,"
            + COLUMN_LOCATION_LONGITUDE + " real not null,"
            + COLUMN_LOCATION_TIME + " text not null"
    + ");";

    public PathSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(SESSIONS_CREATE);
        database.execSQL(PATHS_CREATE);
        database.execSQL(LOCATIONS_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(PathSQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATHS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }
}
