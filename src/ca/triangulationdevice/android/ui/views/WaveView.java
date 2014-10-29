package ca.triangulationdevice.android.ui.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class WaveView extends View {
    private static final float SAMPLING_RATE = 5000f;
    private static final float PADDING = 10f;
    @SuppressWarnings("unused")
    private static final String TAG = "WaveView";

    private final List<Float> frequencies = new ArrayList<Float>();
    private Paint linePaint;
    private List<Path> paths = new ArrayList<Path>();
    private int width;
    private int height;
    private double amplitude = 1d;
    private final List<Double> offsets = new ArrayList<Double>();

    public WaveView(Context context) {
        super(context);
        this.init();
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public WaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.init();
    }

    private void init() {
        this.frequencies.add(0f);
        this.offsets.add(0d);

        this.linePaint = new Paint();
        this.linePaint.setColor(Color.BLACK);
        this.linePaint.setAntiAlias(true);
        this.linePaint.setStyle(Paint.Style.STROKE);
        this.linePaint.setStrokeJoin(Paint.Join.ROUND);
        this.linePaint.setStrokeCap(Cap.SQUARE);
        this.linePaint.setStrokeWidth(6);
    }

    public void clear() {
        this.frequencies.clear();
        this.offsets.clear();

        this.frequencies.add(0f);
        this.offsets.add(0d);
    }

    /**
     * On a ratio from 0 to 1, 1 being max.
     * 
     * @param newAmplitude
     */
    public void setAmplitude(double newAmplitude) {
        this.amplitude = newAmplitude;
        this.redrawPath();
    }

    public void setColor(int color) {
        this.linePaint.setColor(color);
        this.invalidate();
    }

    @Override
    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        this.width = width;
        this.height = height;
        this.redrawPath();
    }

    public void setFrequency(int which, float newFrequency) {
        if (this.frequencies.size() <= which) {
            for (int i = this.frequencies.size(); i <= which; i++) {
                this.frequencies.add(0f);
                this.offsets.add(0d);
            }
        }

        this.frequencies.set(which, newFrequency);
        this.redrawPath();
    }

    public void redrawPath() {
        this.paths = new ArrayList<Path>();
        for (Float frequency : this.frequencies) {
            this.paths.add(new Path());
        }

        int yMiddle = this.height / 2;
        int amplitude = (int) Math.round(this.amplitude * (yMiddle - 2 * PADDING));

        for (int i = 0; i < this.frequencies.size(); i++) {
            float frequency = this.frequencies.get(i);
            this.offsets.set(i, (double) (System.currentTimeMillis() % 1000) * frequency / 2000);
            Path path = this.paths.get(i);
            for (int x = 0; x < this.width; x += 2) {
                float y = (float) (yMiddle + amplitude * Math.sin(frequency * (x + this.offsets.get(i)) / SAMPLING_RATE));
                if (x == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
        }

        this.invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (Path path : this.paths) {
            canvas.drawPath(path, this.linePaint);
        }
        this.redrawPath();
    }
}
