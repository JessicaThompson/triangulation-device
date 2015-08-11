package ca.triangulationdevice.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OvalsView extends View {

    private static final String TAG = "OvalsView";

    private Paint circleStroke;
    private Paint activatedStroke;

    private int size = 0;

    private boolean scaled = false;

    private static final int CIRCLE_COUNT = 4;
    private List<RectF> circles = new ArrayList<>(CIRCLE_COUNT);

    private int activated = -1;

    private CircleChangedListener listener;

    private static final int LOCK_PADDING = 40;
    private static final int PADDING = 20;
    private static final int BETWEEN = 40;
    private float calcPadding;
    private float calcBetween;

    private ScaleGestureDetector zoomDetector;
    private float heightPadding;

    public OvalsView(Context context) {
        super(context);
        init(context);
    }

    public OvalsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OvalsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public OvalsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(final Context context) {
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

        this.reset();

        calcPadding = dpToPixels(PADDING);
        calcBetween = dpToPixels(BETWEEN);

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!scaled) {
                    activated = (activated < (CIRCLE_COUNT - 1)) ? activated + 1 : -1;
                    OvalsView.this.invalidate();
                } else {
                    scaled = false;
                }
            }
        });

        zoomDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            private float lastFocusX = 0;
            private float lastFocusY = 0;

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                lastFocusX = detector.getFocusX();
                lastFocusY = detector.getFocusY();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (activated > -1) {
                    RectF circleBounds = circles.get(activated);
                    float transDx = detector.getFocusX() - lastFocusX;
                    float scaleDx = (circleBounds.width() - detector.getCurrentSpanX()) / 2f;
                    float transDy = detector.getFocusY() - lastFocusY;
                    float scaleDy = (circleBounds.height() - detector.getCurrentSpanY()) / 2f;

                    circleBounds.left = circleBounds.left + scaleDx + transDx;
                    circleBounds.right = circleBounds.right - scaleDx + transDx;
                    circleBounds.top = circleBounds.top + scaleDy + transDy;
                    circleBounds.bottom = circleBounds.bottom - scaleDy + transDy;
                    OvalsView.this.invalidate();

                    listener.onCircleChanged(activated, circleBounds, size);

                    lastFocusX = detector.getFocusX();
                    lastFocusY = detector.getFocusY();
                }

                return false;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                OvalsView.this.scaled = true;
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

            for (int i = 0; i < CIRCLE_COUNT; i++) {
                float between = i * calcBetween;
                circles.get(i).set(calcPadding + between, calcPadding + heightPadding + between, size - (calcPadding + between), heightPadding + size - (calcPadding + between));
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (int i = 0; i < CIRCLE_COUNT; i++) {
            Paint paint = (activated == i) ? activatedStroke : circleStroke;
            canvas.drawOval(circles.get(i), paint);
        }
    }

    public void reset() {
        circles.clear();
        for (int i = 0; i < CIRCLE_COUNT; i++) {
            circles.add(new RectF());

            if (listener != null)
                listener.onCircleChanged(activated, circles.get(i), size);
        }
        this.requestLayout();
        this.invalidate();
    }

    public void setSizeChangedListener(CircleChangedListener listener) {
        this.listener = listener;
    }

    private float dpToPixels(int dps) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps, getResources().getDisplayMetrics());
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        this.zoomDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public interface CircleChangedListener {
        public void onCircleChanged(int index, RectF bounds, int size);
    }
}
