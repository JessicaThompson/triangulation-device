package ca.triangulationdevice.android.ui.views;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import ca.triangulationdevice.android.util.Typefaces;
import de.vndvl.chrs.triangulationdevice.R;

/**
 * A view which shows a waveform and a label showing which device it is and its
 * location.
 */
public class WaveformLabelView extends FrameLayout {

    // private View waveform;
    private TextView deviceLabel;
    private TextView longitudeValue;
    private TextView latitudeValue;
    private LinearLayout label;

    public WaveformLabelView(Context context) {
        super(context);
        init();
    }

    public WaveformLabelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformLabelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.device_audio_view, this);
        Typefaces.loadTypefaces(getContext());

        this.label = (LinearLayout) findViewById(R.id.label);
        this.deviceLabel = (TextView) findViewById(R.id.device_label);
        this.longitudeValue = (TextView) findViewById(R.id.device_long_value);
        this.latitudeValue = (TextView) findViewById(R.id.device_lat_value);

        // Set the custom typefaces.
        this.longitudeValue.setTypeface(Typefaces.ralewayLight);
        this.latitudeValue.setTypeface(Typefaces.ralewayLight);
        this.deviceLabel.setTypeface(Typefaces.raleway);
        TextView longitudeTitle = (TextView) findViewById(R.id.device_long_title);
        TextView latitudeTitle = (TextView) findViewById(R.id.device_lat_title);
        longitudeTitle.setTypeface(Typefaces.ralewaySemiBold);
        latitudeTitle.setTypeface(Typefaces.ralewaySemiBold);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.setMinimumHeight(this.label.getHeight());
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Set the displayed name.
     * 
     * @param deviceName
     *            The device name to show in the label display.
     */
    public void setDeviceName(CharSequence deviceName) {
        this.deviceLabel.setText(deviceName);
    }

    /**
     * Set the location to display.
     * 
     * @param newLocation
     *            The location, whose longitude and latitude displays will be
     *            displayed in the label.
     */
    public void setLocation(Location newLocation) {
        this.longitudeValue.setText(String.format("%.6f", newLocation.getLongitude()));
        this.latitudeValue.setText(String.format("%.6f", newLocation.getLatitude()));
    }
}