package ca.triangulationdevice.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BezierCirclesView extends View {

    private Paint circleStroke;
    private Paint activatedStroke;

    private int size = 0;

    private static final int CIRCLE_COUNT = 1;
    private List<Path> paths = new ArrayList<>(CIRCLE_COUNT);

    private int activated = -1;

    private static final int PADDING = 20;
    private static final int BETWEEN = 40;
    private static final int THING = 23;
    private float calcPadding;
    private float calcBetween;
    private float calcThing;
    private float heightPadding;

    private GestureDetector tapDetector;
    private ScaleGestureDetector zoomDetector;

    public BezierCirclesView(Context context) {
        super(context);
        init(context);
    }

    public BezierCirclesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BezierCirclesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public BezierCirclesView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        circleStroke = new Paint();
        circleStroke.setStyle(Paint.Style.STROKE);
        circleStroke.setStrokeCap(Paint.Cap.ROUND);
        circleStroke.setStrokeWidth(5.0f);
        circleStroke.setAntiAlias(true);
        circleStroke.setColor(Color.WHITE);

        activatedStroke = new Paint();
        activatedStroke.setStyle(Paint.Style.STROKE);
        activatedStroke.setStrokeCap(Paint.Cap.ROUND);
        activatedStroke.setStrokeWidth(15.0f);
        activatedStroke.setAntiAlias(true);
        activatedStroke.setColor(Color.WHITE);

        for (int i = 0; i < CIRCLE_COUNT; i++) {
            paths.add(new Path());
        }

        calcPadding = dpToPixels(PADDING);
        calcBetween = dpToPixels(BETWEEN);
        calcThing = dpToPixels(THING);

        tapDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            public boolean onSingleTapConfirmed(MotionEvent e) {
                activated = (activated == 0) ? -1 : 0;
                return true;
            }
        });

        zoomDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            public boolean onScale(ScaleGestureDetector detector) {
                return false;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY &&
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {

            int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
            int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();

            size = Math.min(width, height);
            heightPadding = (height - size) / 2f;

            float half = size / 2f;
            float quarter = size / 4f;
            float threeQuarter = half + quarter;

            for (int i = 0; i < CIRCLE_COUNT; i++) {
                paths.get(i).reset();

                float inset = i * calcBetween + calcPadding;
                float thing = i * calcThing;

                // Start at the top.
                paths.get(i).moveTo(half, inset + heightPadding);

                // Bezier curve to the right.
                paths.get(i).cubicTo(threeQuarter - thing, inset + heightPadding, size - inset, quarter + thing + heightPadding, size - inset, half + heightPadding);

                // Bezier curve to the bottom.
                paths.get(i).cubicTo(size - inset, threeQuarter - thing + heightPadding, threeQuarter - thing, size - inset + heightPadding, half, size - inset + heightPadding);

                // Bezier curve to the left.
                paths.get(i).cubicTo(quarter + thing, size - inset + heightPadding, inset, threeQuarter - thing + heightPadding, inset, half + heightPadding);

                // Finally, Bezier curve to the top.
                paths.get(i).cubicTo(inset, quarter + thing + heightPadding, quarter + thing, inset + heightPadding, half, inset + heightPadding);
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (int i = 0; i < CIRCLE_COUNT; i++) {
//            canvas.drawPath(paths.get(i), circleStroke);
            Paint paint = (activated == i) ? activatedStroke : circleStroke;
            canvas.drawOval(calcPadding, calcPadding + heightPadding, size - calcPadding, heightPadding + size - calcPadding, circleStroke);
        }
    }

    private float dpToPixels(int dps) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps, getResources().getDisplayMetrics());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.tapDetector.onTouchEvent(event);
        this.zoomDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
