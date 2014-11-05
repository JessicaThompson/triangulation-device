package ca.triangulationdevice.android.ui;

import android.app.Activity;
import android.os.Bundle;
import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.storage.PathStorage;

public class PlayerActivity extends Activity {
    public static final String SESSION_EXTRA = "session";

    private final PathStorage storage = new PathStorage(this);

    // private List<Point> myPath;
    // private List<Point> theirPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        // Load the session we've been given.
        // this.storage.open();
        // List<PathStorage.Session> sessions = this.storage.loadSessions();
        // int sessionId = getIntent().getIntExtra(SESSION_EXTRA, 0);
        // List<PathStorage.Path> sessionPaths = sessions.get(sessionId).paths;
        //
        // try {
        // this.myPath =
        // this.storage.loadPoints(sessionPaths.get(PathStorage.MINE));
        // this.theirPath =
        // this.storage.loadPoints(sessionPaths.get(PathStorage.THEIRS));
        // } catch (ParseException e) {
        // e.printStackTrace();
        // this.finish();
        // }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.storage.close();
    }
}
