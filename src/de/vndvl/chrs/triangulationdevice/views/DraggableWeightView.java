package de.vndvl.chrs.triangulationdevice.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class DraggableWeightView extends LinearLayout {
	
	private View topChild;
	private View bottomChild;
	private boolean active = true;

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
	
	public void onFinishInflate() {
		super.onFinishInflate();
		this.topChild = this.getChildAt(0);
		this.bottomChild = this.getChildAt(1);
	}
	
	public void activate() {
		this.active = true;
	}
	
	public void deactivate() {
		this.active = false;
	}
	
	private void init() {
		this.setOnTouchListener(new OnTouchListener() {
			private float lastY;
			private int lastHeight;
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (active) {
				    switch (event.getAction()) {
				    	case MotionEvent.ACTION_DOWN:
				    		this.lastY = event.getY();
				    		this.lastHeight = DraggableWeightView.this.topChild.getHeight();
				    		break;
				        case MotionEvent.ACTION_MOVE:
						    float y = event.getY();
						    float dy = y - this.lastY;
						    
						    int newHeight = Math.max(Math.min(Math.round(lastHeight + dy), DraggableWeightView.this.getHeight()), 0);
						    Log.i("DraggableWeightView", String.format("%d", newHeight));
						    
						    ViewGroup.LayoutParams params = DraggableWeightView.this.topChild.getLayoutParams();
						    params.height = newHeight;
						    DraggableWeightView.this.topChild.setLayoutParams(params);
						    DraggableWeightView.this.topChild.invalidate();
				        	break;
				    }
				}
			    return true;

			}
			
		});
	}
}
