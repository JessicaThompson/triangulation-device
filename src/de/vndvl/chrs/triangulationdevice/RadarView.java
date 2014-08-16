package de.vndvl.chrs.triangulationdevice;

import java.util.Set;

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

public class RadarView extends ImageView {
	
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
		this.invalidate();
	}
	
	public void setLocation(Location location) {
		this.myLocation = location;
	}
	
	public void setOtherLocation(Location location) {
		this.otherLocation = location;
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
	
	@Override
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
		float leftArrow = (getPaddingLeft() + getWidth()) / 2 - (arrow.getWidth() / 2);
		float topArrow = (getPaddingTop() + getHeight()) / 2 - (arrow.getHeight() / 2);
		canvas.drawBitmap(arrow, leftArrow, topArrow, arrowPaint);
		
		if (otherLocation != null) {
			int canvasWidth = getWidth() - getPaddingLeft() - getPaddingRight();
			float x = (float) (otherLocation.getLongitude() - myLocation.getLongitude()) * canvasWidth;
			float y = (float) (otherLocation.getLatitude() - myLocation.getLatitude()) * canvasWidth;
			if (otherLocation.distanceTo(myLocation) < 1.0f) {
				canvas.drawBitmap(otherMarker.getBitmap(), x, y, arrowPaint);
			}
		}
	}
}