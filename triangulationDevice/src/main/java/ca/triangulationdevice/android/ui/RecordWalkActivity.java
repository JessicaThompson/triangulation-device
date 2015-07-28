package ca.triangulationdevice.android.ui;

import android.app.DialogFragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.ui.dialog.SaveRecordingDialogFragment;
import ca.triangulationdevice.android.ui.partial.RecordingActivity;
import ca.triangulationdevice.android.ui.views.OvalsView;

public class RecordWalkActivity extends RecordingActivity implements SaveRecordingDialogFragment.SaveRecordingDialogListener {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.circles);

        control = (TextView) findViewById(R.id.control);
        OvalsView ovalsView = (OvalsView) findViewById(R.id.circles);
        ovalsView.setSizeChangedListener(new OvalsView.SizeChangedListener() {
            @Override
            public void onSizeChanged(float width, float height) {
                RecordWalkActivity.this.pd.testCircle(width, height);
            }
        });

        handler = new Handler();
    }

    public void toggleRec(View view) {
        if (startTime == null) {
            startTime = new Date();

            // Change the record button to a stop button.
            Drawable stopButton = getResources().getDrawable(R.drawable.stop);
            control.setCompoundDrawablesWithIntrinsicBounds(null, stopButton, null, null);

            // Start PD.
            this.recording = true;
            this.pd.start();

            updateTime.run();
        } else {
            startTime = null;

            // Change the record button back.
            Drawable recordButton = getResources().getDrawable(R.drawable.record);
            control.setCompoundDrawablesWithIntrinsicBounds(null, recordButton, null, null);
            control.setText("REC");

            // Stop PD.
            this.recording = false;
            this.pd.stop();

            handler.removeCallbacks(updateTime);

            this.save();
        }
    }

    private void save() {
        DialogFragment fragment = new SaveRecordingDialogFragment();
        fragment.show(getFragmentManager(), "savewalk");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // Save the walk.
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // Discard the walk.
    }
}
