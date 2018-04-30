package ca.triangulationdevice.android.ui.partial;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;

import java.util.Date;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.model.User;

public abstract class PlaybackRecordingActivity extends RecordingActivity {

    public static final String ID_EXTRA = "sessionid";
    private static final String TAG = "PlaybackRecording";

    protected Session otherSession;
    private Thread playbackThread;
    private PlaybackRunnable playback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User currentUser = this.application.userManager.getCurrentUser();
        setContentView(R.layout.circles);

        Intent intent = getIntent();
        try {
            String id = intent.getStringExtra(ID_EXTRA);
            if (id != null) {
                otherSession = this.application.userManager.getSession(id);
                playback = new PlaybackRunnable(otherSession);
                playbackThread = new Thread(playback);
                playbackThread.start();
            }
        } catch (CouchbaseLiteException e) {
            this.finish();
            Toast.makeText(this, "Could not load session - uh oh?", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Could not load session: " + e.getMessage());
        }
    }

    public void toggleRec(View v) {
        if (!playback.isPlaying()) {
            playback.start();
        } else {
            playback.stop();
            this.pd.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.pd.start();
    }

    @Override
    public void onPause() {
        this.pd.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            playbackThread.join(500);
        } catch (InterruptedException ex) {
            // Don't care.
        }
    }

    private class PlaybackRunnable implements Runnable {
        private int myIndex = 0;
        private Session playbackSession;
        private long startTime = -1;
        private long pathStart = -1;

        public PlaybackRunnable(Session session) {
            this.playbackSession = session;
        }

        @Override
        public void run() {
            Session.Path myPath = playbackSession.paths.get(Session.Path.MINE);

            while (!Thread.currentThread().isInterrupted()) {
                System.out.print(startTime);
                if (startTime > -1) {
                    if (myIndex < myPath.points.size()) {
                        long elapsed = System.currentTimeMillis() - startTime;

                        Session.Path.Point myPoint = myPath.points.get(myIndex);
                        long pathElapsed = myPoint.location.getTime() - pathStart;

                        if (elapsed > pathElapsed) {
                            pd.theirLocationChanged(myPoint.location);
                            myIndex++;
                        }
                    } else {
                        stop();
                    }
                }
            }
        }

        public void start() {
            startTime = System.currentTimeMillis();
            myIndex = 0;
            pathStart = playbackSession.paths.get(Session.Path.MINE).points.get(0).location.getTime();
            Log.i(TAG, "Started, setting current time to " + startTime);
        }

        public void stop() {
            startTime = -1;
        }

        public boolean isPlaying() {
            return startTime > -1;
        }
    }
}
