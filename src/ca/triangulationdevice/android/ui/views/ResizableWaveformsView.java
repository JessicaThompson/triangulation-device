package ca.triangulationdevice.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import de.vndvl.chrs.triangulationdevice.R;

public class ResizableWaveformsView extends LinearLayout {
    private WaveformLabelView theirWaveform;
    private WaveformLabelView myWaveform;
    private LinearLayout border;
    private float density;
    private Listener listener;
    private boolean active = false;

    public ResizableWaveformsView(Context context) {
        super(context);
        init(context);
    }

    public ResizableWaveformsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ResizableWaveformsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void activate() {
        this.active = true;
        this.theirWaveform.setVisibility(View.VISIBLE);
    }

    public void deactivate() {
        this.active = false;
        this.theirWaveform.setVisibility(View.GONE);
    }

    @Override
    public void onFinishInflate() {
        this.myWaveform = (WaveformLabelView) this.findViewById(R.id.my_waveform);
        this.theirWaveform = (WaveformLabelView) this.findViewById(R.id.their_waveform);
        this.border = (LinearLayout) this.myWaveform.findViewById(R.id.label);

        this.border.setOnTouchListener(new OnTouchListener() {
            private final float minHeight = 180;
            private final float maxHeight = 1000;

            private int offset;
            private float lastY;
            private float lastHeight;
            private final View myWaveformView = ResizableWaveformsView.this.myWaveform;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (ResizableWaveformsView.this.active) {
                    final int[] location = new int[2];
                    ResizableWaveformsView.this.border.getLocationOnScreen(location);
                    this.offset = location[1];

                    switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        this.lastY = event.getY();
                        this.lastHeight = this.myWaveformView.getHeight();
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

            // y is relative to the top of the border View.
            private void updateWeight(float y) {
                float dy = (y + this.offset - this.lastY);
                int newHeight = Math.round(Math.min(this.maxHeight, Math.max(this.lastHeight + dy, this.minHeight)));

                ResizableWaveformsView.this.listener.onChanged((newHeight - this.minHeight) / (this.maxHeight - this.minHeight));

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) this.myWaveformView.getLayoutParams();
                params.height = newHeight;
                this.myWaveformView.setLayoutParams(params);
                ResizableWaveformsView.this.invalidate();
            }
        });
    }

    private void init(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        this.density = metrics.densityDpi / 160f;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        public void onChanged(double topbottomratio);
    }
}
