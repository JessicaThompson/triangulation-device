package ca.triangulationdevice.android.ui;


import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.ui.dialog.ConfirmSaveRecordingDialogFragment;
import ca.triangulationdevice.android.ui.dialog.DialogListener;
import ca.triangulationdevice.android.ui.dialog.SaveRecordingDialogFragment;
import ca.triangulationdevice.android.ui.partial.PlaybackRecordingActivity;
import ca.triangulationdevice.android.ui.views.OvalsView;
import ca.triangulationdevice.android.util.GetCityTask;
import de.hdodenhof.circleimageview.CircleImageView;

public class PlaybackWalkActivity extends PlaybackRecordingActivity {

    private final static String TAG = "PlaybackWalkActivity";
    private final static int DELAY = 1000;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("mm:ss", Locale.getDefault());
    private static final long TIMEZONE_OFFSET = TimeZone.getDefault().getRawOffset();

    private TextView control;

    private Handler handler;
    private Date startTime = null;

    Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            Date runTime = new Date();
            long diff = runTime.getTime() - startTime.getTime();
            control.setText(DATE_FORMAT.format(new Date(diff - TIMEZONE_OFFSET)));
            handler.postDelayed(updateTime, DELAY);
        }
    };
    private OvalsView ovalsView;
    private CircleImageView circleImageView;
    private TextView connectedName;
    private TextView connectedLocation;
    private LinearLayout connectedInfo;
    private long lastUserLocationUpdateTime = System.currentTimeMillis();
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.circles);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        control = (TextView) findViewById(R.id.control);
        ovalsView = (OvalsView) findViewById(R.id.circles);
        ovalsView.setSizeChangedListener(this.pd);

        connectedInfo = (LinearLayout) findViewById(R.id.connected_info);
        circleImageView = (CircleImageView) findViewById(R.id.connected_profile_image);
        connectedName = (TextView) findViewById(R.id.connected_name);
        connectedLocation = (TextView) findViewById(R.id.connected_location);

        connectedInfo.setVisibility(View.VISIBLE);
        connectedName.setText(otherSession.title);
        connectedLocation.setText("");

        mapView = (MapView) findViewById(R.id.map);

        handler = new Handler();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.pd.start();
    }

    @Override
    public void onConnected(Bundle connectionHunt) {
        super.onConnected(connectionHunt);

        // Zoom the map to our position!
        LatLng latLng = new LatLng(getLocation());
        mapView.setCenter(latLng);
    }

    @Override
    public void onPause() {
        this.stop();
        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUserLocationUpdateTime > 1000 * 60) {
            final String currentId = application.userManager.getCurrentUser().id;
            try {
                final User current = application.userManager.getUser(currentId);

                // Update the location string, only if we've moved 100m.
                current.myLocation = location;
                GetCityTask task = new GetCityTask(this, location) {
                    @Override
                    protected void onPostExecute(String result) {
                        current.location = result;
                        try {
                            application.userManager.add(current);
                        } catch (CouchbaseLiteException e) {
                            e.printStackTrace();
                        }
                    }
                };
                task.execute();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
    }

    public void reset(View v) {
        ovalsView.reset();
    }

    @Override
    public void toggleRec(View view) {
        super.toggleRec(view);
        if (startTime == null) {
            startTime = new Date();

            // Change the record button to a stop button.
            Drawable stopButton = getResources().getDrawable(R.drawable.stop);
            control.setCompoundDrawablesWithIntrinsicBounds(null, stopButton, null, null);

            // Start PD.
            this.startRecording(application.userManager.getCurrentUser());
            this.pd.start();

            updateTime.run();
        } else {
            startTime = null;

            // Change the record button back.
            Drawable recordButton = getResources().getDrawable(R.drawable.record);
            control.setCompoundDrawablesWithIntrinsicBounds(null, recordButton, null, null);
            control.setText("REC");

            // Stop PD.
            this.pd.stop();

            handler.removeCallbacks(updateTime);

            this.save();
        }
    }

    private void save() {
        ConfirmSaveRecordingDialogFragment confirmFragment = new ConfirmSaveRecordingDialogFragment();
        final SaveRecordingDialogFragment fragment = new SaveRecordingDialogFragment();

        confirmFragment.setListener(new DialogListener() {
            @Override
            public void onDialogPositiveClick(String title, String description) {
                fragment.show(getFragmentManager(), "savewalk");
            }

            @Override
            public void onDialogNegativeClick() {
                // Discard the walk.
            }
        });
        fragment.setListener(new DialogListener() {
            @Override
            public void onDialogPositiveClick(String title, String description) {
                try {
                    PlaybackWalkActivity.this.save(UUID.randomUUID().toString(), title, description);
                } catch (IOException e) {
                    Log.e(TAG, "Error while saving recording: " + e.getMessage());
                    Toast.makeText(PlaybackWalkActivity.this, "Error saving walk audio.", Toast.LENGTH_SHORT).show();
                } catch (CouchbaseLiteException e) {
                    Log.e(TAG, "Error while saving path: " + e.getMessage());
                    Toast.makeText(PlaybackWalkActivity.this, "Error saving walk path.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDialogNegativeClick() {
                // Discard the walk.
            }
        });

        confirmFragment.show(getFragmentManager(), "confirmsavewalk");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
