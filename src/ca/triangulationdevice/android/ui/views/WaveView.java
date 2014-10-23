package ca.triangulationdevice.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class WaveView extends View {
    private static final float SAMPLING_RATE = 44100f;
    private static final float PADDING = 10f;
    @SuppressWarnings("unused")
    private static final String TAG = "WaveView";

    private float frequency = 0;// (float) Math.random() * 5000f;
    private Paint linePaint;
    private Path path = new Path();
    private int width;
    private int height;
    private int offset;

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
        this.linePaint = new Paint();
        this.linePaint.setColor(Color.BLACK);
        this.linePaint.setAntiAlias(true);
        this.linePaint.setStyle(Paint.Style.STROKE);
        this.linePaint.setStrokeJoin(Paint.Join.ROUND);
        this.linePaint.setStrokeCap(Cap.SQUARE);
        this.linePaint.setStrokeWidth(12);
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

    public void setFrequency(float newFrequency) {
        this.frequency = newFrequency;
        this.redrawPath();
    }

    public void redrawPath() {
        this.path = new Path();

        int yMiddle = this.height / 2;
        int amplitude = Math.round(yMiddle - 2 * PADDING);

        this.offset = 0;// (int) Math.round(Math.random() * 5);
        for (int x = 0; x < this.width; x += 2) {
            float y = (float) (yMiddle + amplitude * Math.sin(this.frequency * (x + this.offset) / SAMPLING_RATE));
            if (x == 0) {
                this.path.moveTo(x, y);
            } else {
                this.path.lineTo(x, y);
            }
        }

        this.invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawPath(this.path, this.linePaint);
        this.redrawPath();
    }
}
