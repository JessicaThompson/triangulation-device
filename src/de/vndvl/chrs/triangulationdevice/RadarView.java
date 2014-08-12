package de.vndvl.chrs.triangulationdevice;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RadarView extends ImageView {
		
	public RadarView(Context context) {
		super(context);
		init();
	}
	
	public RadarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public RadarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	@SuppressWarnings("deprecation") // setBackgroundDrawable is depracated, but backwards compatibility.
	private void init() {
		Resources res = getResources();
		ShapeDrawable radarBackground = (ShapeDrawable) res.getDrawable(R.drawable.radar);
		this.setBackgroundDrawable(radarBackground);
	}
}