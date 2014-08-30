package de.vndvl.chrs.triangulationdevice.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.util.AttributeSet;
import android.widget.ImageView;
import de.vndvl.chrs.triangulationdevice.R;

public class RadarView extends ImageView {
	
	private static final float SCALING = 0.02f;
	
	private float azimuth = 0;
	private boolean connected = false;
	private Paint connectedPaint;
	private Paint disconnectedPaint;
	private Paint arrowPaint;
	private BitmapDrawable arrowBlack;
	private BitmapDrawable arrowGreen;
	private BitmapDrawable otherMarker;
	
	private RectF boundsRect;
	
	private Location myLocation;
	private Location otherLocation;
	
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
	
	public void connected(boolean connected) {
		this.connected = connected;
		
		if (connected == false) {
		    this.otherLocation = null;
		}
		
		this.invalidate();
	}
	
	public void setAzimuth(float azimuth) {
	    this.azimuth = azimuth;
	}
	
	public void setLocation(Location location) {
		this.myLocation = location;
		this.invalidate();
	}
	
	public void setOtherLocation(Location location) {
		this.otherLocation = location;
		this.invalidate();
	}
	
	@SuppressWarnings("deprecation") // setBackgroundDrawable is deprecated, but backwards compatibility.
	private void init() {
		Resources res = getResources();
		LayerDrawable radarBackground = (LayerDrawable) res.getDrawable(R.drawable.radar);
		arrowBlack = (BitmapDrawable) res.getDrawable(R.drawable.arrow_black);
		arrowGreen = (BitmapDrawable) res.getDrawable(R.drawable.arrow_green);
		otherMarker = (BitmapDrawable) res.getDrawable(R.drawable.paired_device);
		this.setBackgroundDrawable(radarBackground);

		connectedPaint = new Paint();
		connectedPaint.setStrokeWidth(10f);
		connectedPaint.setColor(Color.GREEN);
		connectedPaint.setStyle(Style.FILL_AND_STROKE);

		disconnectedPaint = new Paint(connectedPaint);

		arrowPaint = new Paint();
		
		boundsRect = new RectF(0, 0, 0, 0);
	}
	
	@SuppressLint("DrawAllocation") @Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int originalWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        int calculatedHeight = originalWidth;
        int finalWidth, finalHeight;
        if (calculatedHeight > originalHeight) {
            finalWidth = originalHeight; 
            finalHeight = originalHeight;
        } else {
            finalWidth = originalWidth;
            finalHeight = calculatedHeight;
        }
        
        LinearGradient disconnectedGradient = new LinearGradient(getWidth() / 2, getHeight() / 2, getWidth() / 2, 0, getResources().getColor(R.color.radar_red), getResources().getColor(R.color.radar_clear), Shader.TileMode.CLAMP);
		disconnectedPaint.setShader(disconnectedGradient);
		
		LinearGradient connectedGradient = new LinearGradient(getWidth() / 2, getHeight() / 2, getWidth() / 2, 0, getResources().getColor(R.color.radar_green), getResources().getColor(R.color.radar_clear), Shader.TileMode.CLAMP);
		connectedPaint.setShader(connectedGradient);

        super.onMeasure(MeasureSpec.makeMeasureSpec(finalWidth + getPaddingLeft() + getPaddingRight(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(finalHeight + getPaddingTop() + getPaddingBottom(), MeasureSpec.EXACTLY));
    }
	
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		Paint arcPaint = connected? connectedPaint : disconnectedPaint;
		boundsRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
		canvas.drawArc(boundsRect, 245f, 50f, true, arcPaint);
		
		Bitmap arrow = (connected? arrowGreen : arrowBlack).getBitmap();
		float centerX = (getPaddingLeft() + getWidth()) / 2;
		float centerY = (getPaddingTop() + getHeight()) / 2;
		float leftArrow = centerX - (arrow.getWidth() / 2);
		float topArrow = centerY - (arrow.getHeight() / 2);
		canvas.drawBitmap(arrow, leftArrow, topArrow, arrowPaint);
		
		if (connected && otherLocation != null) {
			Bitmap markerBitmap = otherMarker.getBitmap();
			float distance = myLocation.distanceTo(otherLocation); // in metres
			if (distance < 100.0f) {
				// This is an absolute bearing, relative to perfect north.
				double bearing = Math.toRadians(myLocation.bearingTo(otherLocation));
				double cosine = Math.cos(bearing + azimuth);
				double sine = Math.sin(bearing + azimuth);
				double drawDistance = centerX * (1 - Math.exp(-(SCALING * distance)));
				double expx = centerX + drawDistance * cosine - (markerBitmap.getWidth() / 2);
				double expy = centerY + drawDistance * sine - (markerBitmap.getHeight() / 2);
				canvas.drawBitmap(otherMarker.getBitmap(), (float) expx, (float) expy, arrowPaint);
			}
		}
	}
}