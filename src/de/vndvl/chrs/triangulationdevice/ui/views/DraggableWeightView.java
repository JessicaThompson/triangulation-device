package de.vndvl.chrs.triangulationdevice.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.vndvl.chrs.triangulationdevice.R;

/**
 * A {@link View} which has two children and lets the user drag the line between
 * them to adjust their relative size.
 * 
 * {@link #setListener(Listener)} accepts a {@link Listener} to call when the
 * value is changed, in case other classes want to use the value for something.
 */
public class DraggableWeightView extends LinearLayout {
    @SuppressWarnings("unused")
    private final static String TAG = "DraggableWeightView";

    private View topChild = null;
    private View bottomChild = null;
    private View topLabel = null;
    private boolean active = true;
    private Listener listener = null;
    private int minHeight = 0;
    private int maxHeight = 0;

    public DraggableWeightView(Context context) {
        super(context);
        init();
    }

    public DraggableWeightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DraggableWeightView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        init();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.topChild = this.getChildAt(0);
        this.bottomChild = this.getChildAt(1);
        this.topLabel = this.topChild.findViewById(R.id.label);

        if (this.bottomChild.getVisibility() != View.VISIBLE) {
            this.active = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.minHeight = 2 * this.topLabel.getHeight();
        this.maxHeight = 2 * (bottom - top) - this.minHeight; // Subtract
                                                              // minHeight for
                                                              // the second
                                                              // label
    }

    /**
     * Add a {@link Listener} to be passed the ratio between top and bottom when
     * it's changed.
     * 
     * @param listener
     *            The Listener to call.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Enable draggability.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Disable draggability.
     */
    public void deactivate() {
        this.active = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        this.setOnTouchListener(new OnTouchListener() {
            private float lastY;
            private int lastHeight;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (DraggableWeightView.this.active) {
                    switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        this.lastY = event.getY();
                        this.lastHeight = DraggableWeightView.this.topChild.getHeight();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        updateWeight(event.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                        updateWeight(event.getY());
                        this.lastY = 0;
                        break;
                    }
                }
                return true;
            }

            private void updateWeight(float y) {
                float dy = y - this.lastY;
                int newHeight = Math.max(Math.min(Math.round(this.lastHeight + dy), DraggableWeightView.this.maxHeight), DraggableWeightView.this.minHeight);

                double ratio = (double) (newHeight - DraggableWeightView.this.minHeight) / (double) (DraggableWeightView.this.maxHeight - DraggableWeightView.this.minHeight);
                DraggableWeightView.this.listener.onChanged(ratio);

                ViewGroup.LayoutParams params = DraggableWeightView.this.topChild.getLayoutParams();
                params.height = newHeight;
                DraggableWeightView.this.topChild.setLayoutParams(params);
                DraggableWeightView.this.topChild.invalidate();
            }
        });
    }

    public interface Listener {
        public void onChanged(double topBottomRatio);
    }
}
