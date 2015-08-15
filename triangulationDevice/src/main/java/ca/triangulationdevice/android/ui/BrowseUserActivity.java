package ca.triangulationdevice.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.ipc.StringZeroMQClient;
import ca.triangulationdevice.android.ipc.StringZeroMQServer;
import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.pd.Triangulation2Driver;
import ca.triangulationdevice.android.storage.CouchDBUserManager;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.ui.marker.SessionMarker;
import ca.triangulationdevice.android.ui.marker.UserMarker;
import ca.triangulationdevice.android.ui.partial.LocationActivity;
import ca.triangulationdevice.android.ui.partial.NetworkRecordingActivity;
import ca.triangulationdevice.android.ui.partial.PlaybackRecordingActivity;
import ca.triangulationdevice.android.util.NetworkUtils;
import ca.triangulationdevice.android.util.Typefaces;

/**
 * An {@link Activity} which lets the user create sessions, connect with other
 * devices, and see the current status of the other connected user.
 */
public class BrowseUserActivity extends LocationActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "BrowseUserActivity";
    private static final float DEFAULT_ZOOM = 16;

    private Resources resources;
    private final Map<Marker, User> markerUserMap = new HashMap<>();
    private final Map<Marker, Session> markerSessionMap = new HashMap<>();

    private MapView mapView;
    private ViewGroup miniPlayer;
    private ViewGroup miniProfile;

    private ImageView playerControl;

    private ImageView miniProfileImage;
    private TextView miniName;
    private TextView miniLocation;
    private TextView miniDescription;

    private CouchDBUserManager userManager;
    private User focusOtherUser;
    private Session focusOtherSession;

    Handler transferHandler = new Handler(new BrowseUserHandler());
    StringZeroMQServer zeroServer = new StringZeroMQServer(transferHandler);
    Thread stringThread = new Thread(zeroServer);

    private static final int DELAY = 5 * 60 * 1000;

    Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            User me = userManager.getCurrentUser();
            if (me != null) {
                me.ip = NetworkUtils.getIPAddress(true);
                try {
                    userManager.add(me);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
                handler.postDelayed(updateTime, DELAY);
            }
        }
    };
    private Handler handler;
    private MediaPlayer mediaPlayer;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayUseLogoEnabled(false);

        userManager = application.userManager;

        Typefaces.loadTypefaces(this);
        setContentView(R.layout.map_activity);
        this.resources = getResources();

        mapView = (MapView) this.findViewById(R.id.mapview);
        miniPlayer = (ViewGroup) findViewById(R.id.mini_playback);
        miniProfile = (ViewGroup) findViewById(R.id.mini_profile);

        playerControl = (ImageView) miniPlayer.findViewById(R.id.player_control);
        progressBar = (SeekBar) miniPlayer.findViewById(R.id.playback);

        miniProfileImage = (ImageView) miniProfile.findViewById(R.id.mini_profile_image);
        miniName = (TextView) miniProfile.findViewById(R.id.mini_name);
        miniLocation = (TextView) miniProfile.findViewById(R.id.mini_location);
        miniDescription = (TextView) miniProfile.findViewById(R.id.mini_walk_description);

        // Setup the map and user overlay.
        UserLocationOverlay myLocationOverlay = new UserLocationOverlay(new GpsLocationProvider(this), mapView);
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        mapView.getOverlays().add(myLocationOverlay);

        // Add the existing users to the map overlay.
        this.addUsers();

        handler = new Handler();

        mapView.setMapViewListener(new MapViewListener() {
            @Override
            public void onShowMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onHideMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onTapMarker(MapView mapView, Marker marker) {
                if (markerUserMap.containsKey(marker)) {
                    User tappedUser = markerUserMap.get(marker);

                    if (tappedUser == focusOtherUser) {
                        openProfile(tappedUser);
                    } else {
                        showMiniProfile(tappedUser);
                    }
                } else {
                    Session tappedSession = markerSessionMap.get(marker);
                    showMiniSession(tappedSession);
                }

            }

            @Override
            public void onLongPressMarker(MapView mapView, Marker marker) {

            }

            @Override
            public void onTapMap(MapView mapView, ILatLng iLatLng) {
                hideMiniProfile();
            }

            @Override
            public void onLongPressMap(MapView mapView, ILatLng iLatLng) {

            }
        });

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTime.run();
        if (!this.stringThread.isAlive())
            this.stringThread.start();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(updateTime);
        try {
            this.stringThread.join(500);
        } catch (InterruptedException e) {
            // Don't care.
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_profile:
                openProfile(this.userManager.getCurrentUser());
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (focusOtherUser != null) {
            this.hideMiniProfile();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        // Zoom the map to our position!
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mapView.setCenter(latLng);
        this.userManager.getCurrentUser().myLocation = location;
        try {
            this.userManager.add(this.userManager.getCurrentUser());
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public File load(String filename) {
        return new File(this.getFilesDir() + "/" + filename + "." + Triangulation2Driver.AUDIO_SUFFIX);
    }

    public void loadCurrent() throws IOException {
        playerControl.setEnabled(false);
        progressBar.setIndeterminate(true);

        File file = load(this.focusOtherSession.audioFilename);
        FileInputStream fisAudio = new FileInputStream(file);
        this.mediaPlayer.setDataSource(fisAudio.getFD());
        this.mediaPlayer.prepareAsync();
        this.mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                playerControl.setEnabled(true);
                progressBar.setIndeterminate(false);
            }
        });
        this.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                progressBar.setProgress(0);
                playerControl.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            }
        });
    }

    public void openCurrentProfile(View v) {
        if (focusOtherUser != null) openProfile(focusOtherUser);
    }

    private void openProfile(User user) {
        Intent intent = new Intent(BrowseUserActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_USER_ID, user.id);
        startActivity(intent);
    }

    private void showMiniSession(Session session) {
        this.focusOtherSession = session;
        try {
            this.focusOtherUser = this.userManager.getUser(session.ownerId);

            if (this.focusOtherUser.picture != null) {
                this.miniProfileImage.setImageDrawable(this.focusOtherUser.picture);
                this.miniProfileImage.setVisibility(View.VISIBLE);
            } else {
                this.miniProfileImage.setVisibility(View.GONE);
            }
        } catch (CouchbaseLiteException ex) {
            // Dunno the user, fuck it.
        }

        this.miniName.setText(session.title);
        this.miniLocation.setText("");
        this.miniDescription.setText(session.description);

        miniProfile.setVisibility(View.VISIBLE);
        if (this.focusOtherSession.ownerId.equals(userManager.getCurrentUser().id)) {
            try {
                loadCurrent();
                miniPlayer.setVisibility(View.VISIBLE);
            } catch (IOException ex) {
                Log.e(TAG, "Couldn't load audio file " + session.audioFilename + ".wav");
            }
        }
    }

    private void showMiniProfile(User user) {
        this.focusOtherUser = user;

        if (user.picture != null) {
            this.miniProfileImage.setImageDrawable(user.picture);
            this.miniProfileImage.setVisibility(View.VISIBLE);
        } else {
            this.miniProfileImage.setVisibility(View.GONE);
        }
        this.miniName.setText(user.name);
        this.miniLocation.setText(user.location);
        this.miniDescription.setText(user.description);

        miniPlayer.setVisibility(View.VISIBLE);
        miniProfile.setVisibility(View.VISIBLE);
    }

    private void hideMiniProfile() {
        focusOtherUser = null;
        focusOtherSession = null;
        miniPlayer.setVisibility(View.GONE);
        miniProfile.setVisibility(View.GONE);
    }

    private void addUsers() {
        try {
            for (User user : application.userManager.getUsers()) {
                if (user != application.userManager.getCurrentUser() && user.myLocation != null) {
                    this.addUserMarker(user);
                }
            }

            for (Session session : application.userManager.getSessions()) {
                this.addSessionMarker(session);
            }
        } catch (CouchbaseLiteException ex) {
            Log.e(TAG, "Unable to hit database: " + ex.getMessage());
        }
    }

    private void addSessionMarker(Session session) {
        Marker marker = new SessionMarker(mapView, session, resources);
        this.markerSessionMap.put(marker, session);
        mapView.addMarker(marker);
    }

    private void addUserMarker(User user) {
        Marker marker = new UserMarker(mapView, user, resources);
        this.markerUserMap.put(marker, user);
        mapView.addMarker(marker);
    }

    private void openNetworked(String id) {
        Log.i(TAG, "Opening circles for " + id);
        Intent intent = new Intent(this, RecordWalkActivity.class);
        intent.putExtra(NetworkRecordingActivity.ID_EXTRA, id);
        startActivity(intent);
    }

    private void openArchived(String id) {
        Log.i(TAG, "Opening circles for " + id);
        Intent intent = new Intent(this, PlaybackWalkActivity.class);
        intent.putExtra(PlaybackRecordingActivity.ID_EXTRA, id);
        startActivity(intent);
    }

    public void connect(View v) {
        if (focusOtherSession == null) {
            new StringZeroMQClient(focusOtherUser.ip).execute(userManager.getCurrentUser().id);
            openNetworked(focusOtherUser.id);
        } else {
            openArchived(focusOtherSession.id);
        }
    }

    public void playerControl(View v) {
        if (this.mediaPlayer.isPlaying()) {
            this.mediaPlayer.pause();
            this.playerControl.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        } else {
            this.mediaPlayer.start();
            this.playerControl.setImageResource(R.drawable.ic_pause_white_24dp);
        }
    }

    // A Handler to receive messages from another BluetoothIPCActivity.
    private class BrowseUserHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            Log.i(TAG, "Got a message" + msg.toString());
            @SuppressWarnings("unchecked") String userID = (String) msg.obj;
            openNetworked(userID);
            return true;
        }
    }
}
