package de.vndvl.chrs.triangulationdevice.ui;

import java.util.List;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.storage.PathStorage;

public class PlayerActivity extends Activity {
    public static final String SESSION_EXTRA = "session";

    private PathStorage storage = new PathStorage(this);
    private List<Location> myPath;
    private List<Location> theirPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        // Load the session we've been given.
        storage.open();
        List<PathStorage.Session> sessions = storage.loadSessions();
        int sessionId = getIntent().getIntExtra(SESSION_EXTRA, 0);
        List<PathStorage.Path> sessionPaths = sessions.get(sessionId).paths;

        myPath = storage.loadPoints(sessionPaths.get(PathStorage.MINE));
        theirPath = storage.loadPoints(sessionPaths.get(PathStorage.THEIRS));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        storage.close();
    }
}
