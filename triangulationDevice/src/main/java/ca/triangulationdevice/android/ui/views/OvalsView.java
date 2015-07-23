package ca.triangulationdevice.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OvalsView extends View {

    private Paint circleStroke;
    private Paint activatedStroke;

    private int size = 0;

    private boolean scaled = false;

    private static final int CIRCLE_COUNT = 1;
    private List<RectF> circles = new ArrayList<>(CIRCLE_COUNT);

    private int activated = -1;

    private SizeChangedListener listener;

    private static final int SCALE = 30;

    private static final int PADDING = 20;
    private static final int BETWEEN = 40;
    private static final int MIN_SIZE = 50;
    private float calcPadding;
    private float calcBetween;
    private float heightPadding;
    private float calcMinSize;

    private GestureDetector tapDetector;
    private ScaleGestureDetector zoomDetector;

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

        for (int i = 0; i < CIRCLE_COUNT; i++) {
            circles.add(new RectF());
        }

        calcPadding = dpToPixels(PADDING);
        calcBetween = dpToPixels(BETWEEN);
        calcMinSize = dpToPixels(MIN_SIZE);

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!scaled) {
                    activated = (activated != 0) ? 0 : -1;
                    OvalsView.this.invalidate();
                } else {
                    scaled = false;
                }
            }
        });

        zoomDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            public boolean onScale(ScaleGestureDetector detector) {
                if (activated > -1) {
                    RectF circleBounds = circles.get(activated);

                    float dx = (circleBounds.width() - detector.getCurrentSpanX()) / 2f;
                    circleBounds.left = circleBounds.left + dx;
                    circleBounds.right = circleBounds.right - dx;

                    float dy = (circleBounds.height() - detector.getCurrentSpanY()) / 2f;
                    circleBounds.top = circleBounds.top + dy;
                    circleBounds.bottom = circleBounds.bottom - dy;
                    OvalsView.this.invalidate();

                    float scaledWidth = circleBounds.width() / size * SCALE;
                    float scaledHeight = circleBounds.height() / size * SCALE;
                    listener.onSizeChanged(scaledWidth, scaledHeight);
                }

                return false;
            }

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
                circles.get(i).set(calcPadding, calcPadding + heightPadding, size - calcPadding, heightPadding + size - calcPadding);
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

    public void setSizeChangedListener(SizeChangedListener listener) {
        this.listener = listener;
    }

    private float dpToPixels(int dps) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dps, getResources().getDisplayMetrics());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.zoomDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public interface SizeChangedListener {
        public void onSizeChanged(float width, float height);
    }
}
