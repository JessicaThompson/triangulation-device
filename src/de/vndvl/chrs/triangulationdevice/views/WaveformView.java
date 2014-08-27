package de.vndvl.chrs.triangulationdevice.views;

import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.Typefaces;
import de.vndvl.chrs.triangulationdevice.R.id;
import de.vndvl.chrs.triangulationdevice.R.layout;
import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class WaveformView extends FrameLayout {
	
	private View waveform;
	private TextView deviceLabel;
	private TextView longitudeValue;
	private TextView latitudeValue;
	
	public WaveformView(Context context) {
        super(context);
        init();
    }
	
    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    private void init() {
        inflate(getContext(), R.layout.device_audio_view, this);
        Typefaces.loadTypefaces(getContext());
        
        waveform = findViewById(R.id.waveform);
        deviceLabel = (TextView) findViewById(R.id.device_label);
        longitudeValue = (TextView) findViewById(R.id.device_long_value);
        latitudeValue = (TextView) findViewById(R.id.device_lat_value);
        
        // Set the custom typefaces.
        longitudeValue.setTypeface(Typefaces.ralewayLight);
        latitudeValue.setTypeface(Typefaces.ralewayLight);
        deviceLabel.setTypeface(Typefaces.raleway);
        TextView longitudeTitle = (TextView) findViewById(R.id.device_long_title);
		TextView latitudeTitle = (TextView) findViewById(R.id.device_lat_title);
        longitudeTitle.setTypeface(Typefaces.ralewaySemiBold);
		latitudeTitle.setTypeface(Typefaces.ralewaySemiBold);
    }
    
    public void setDeviceName(CharSequence deviceName) {
    	this.deviceLabel.setText(deviceName);
    }
    
    public void setLocation(Location newLocation) {
    	longitudeValue.setText(String.format("%.6f", newLocation.getLongitude()));
		latitudeValue.setText(String.format("%.6f", newLocation.getLatitude()));
    }
}