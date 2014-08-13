package de.vndvl.chrs.triangulationdevice;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RadarView extends ImageView {
	
	private boolean connected = false;
	private Paint connectedPaint;
	private Paint disconnectedPaint;
	
	private RectF boundsRect;
	
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
	}
	
	@SuppressWarnings("deprecation") // setBackgroundDrawable is deprecated, but backwards compatibility.
	private void init() {
		Resources res = getResources();
		LayerDrawable radarBackground = (LayerDrawable) res.getDrawable(R.drawable.radar);
		this.setBackgroundDrawable(radarBackground);

		connectedPaint = new Paint();
		connectedPaint.setStrokeWidth(10f);
		connectedPaint.setColor(Color.GREEN);
		connectedPaint.setStyle(Style.FILL_AND_STROKE);

		disconnectedPaint = new Paint(connectedPaint);

		boundsRect = new RectF(0, 0, 0, 0);
	}
	
	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
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

        super.onMeasure(MeasureSpec.makeMeasureSpec(finalWidth + getPaddingLeft() + getPaddingRight(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(finalHeight + getPaddingTop() + getPaddingBottom(), MeasureSpec.EXACTLY));
    }
	
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		Paint arcPaint = connected? connectedPaint : disconnectedPaint;
		boundsRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
		canvas.drawArc(boundsRect, 240f, 60f, true, arcPaint);
	}
}