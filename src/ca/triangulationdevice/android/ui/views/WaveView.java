package ca.triangulationdevice.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class WaveView extends View {
    private float frequency;

    public WaveView(Context context) {
        super(context);
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFrequency(float newFrequency) {
        this.frequency = newFrequency;
    }
}
