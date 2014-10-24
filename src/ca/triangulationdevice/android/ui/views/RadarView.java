package ca.triangulationdevice.android.ui.views;

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
import android.view.View;
import android.widget.ImageView;
import ca.triangulationdevice.android.ui.partial.CompassActivity;
import de.vndvl.chrs.triangulationdevice.R;

/**
 * A {@link View} that, when "connected" to another device, shows a
 * radar-inspired display of two locations.
 */
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

    /**
     * Update the connected state to reflect that in the UI.
     * 
     * @param connected
     *            Whether it's connected or not.
     */
    public void connected(boolean connected) {
        this.connected = connected;

        if (connected == false) {
            this.otherLocation = null;
        }

        this.invalidate();
    }

    /**
     * Update the azimuth of the current user. All {@link Location} calculates
     * their bearing in absolute terms, so this can be used as an offset to
     * simulate the user's phone turning.
     * 
     * @param azimuth
     *            The new azimuth value, with direction and units as per
     *            {@link CompassActivity}.
     */
    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
        this.invalidate();
    }

    /**
     * Set our location value.
     * 
     * @param location
     *            The {@link Location} to use for us.
     */
    public void setLocation(Location location) {
        this.myLocation = location;
        this.invalidate();
    }

    /**
     * Set the other person's location value.
     * 
     * @param location
     *            The {@link Location} to use for "them."
     */
    public void setOtherLocation(Location location) {
        this.otherLocation = location;
        this.invalidate();
    }

    /**
     * @return my location.
     */
    public Location getMyLocation() {
        return this.myLocation;
    }

    /**
     * @return their location.
     */
    public Location getOtherLocation() {
        return this.otherLocation;
    }

    @SuppressWarnings("deprecation")
    // setBackgroundDrawable is deprecated, but backwards compatibility.
    private void init() {
        Resources res = getResources();
        LayerDrawable radarBackground = (LayerDrawable) res.getDrawable(R.drawable.radar);
        this.arrowBlack = (BitmapDrawable) res.getDrawable(R.drawable.arrow_black);
        this.arrowGreen = (BitmapDrawable) res.getDrawable(R.drawable.arrow_green);
        this.otherMarker = (BitmapDrawable) res.getDrawable(R.drawable.paired_device);
        this.setBackgroundDrawable(radarBackground);

        this.connectedPaint = new Paint();
        this.connectedPaint.setStrokeWidth(10f);
        this.connectedPaint.setColor(Color.GREEN);
        this.connectedPaint.setStyle(Style.FILL_AND_STROKE);

        this.disconnectedPaint = new Paint(this.connectedPaint);

        this.arrowPaint = new Paint();

        this.boundsRect = new RectF(0, 0, 0, 0);
    }

    @SuppressLint("DrawAllocation")
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
        this.disconnectedPaint.setShader(disconnectedGradient);

        LinearGradient connectedGradient = new LinearGradient(getWidth() / 2, getHeight() / 2, getWidth() / 2, 0, getResources().getColor(R.color.radar_green), getResources().getColor(R.color.radar_clear), Shader.TileMode.CLAMP);
        this.connectedPaint.setShader(connectedGradient);

        super.onMeasure(MeasureSpec.makeMeasureSpec(finalWidth + getPaddingLeft() + getPaddingRight(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(finalHeight + getPaddingTop() + getPaddingBottom(), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint arcPaint = this.connected ? this.connectedPaint : this.disconnectedPaint;
        this.boundsRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        canvas.drawArc(this.boundsRect, 245f, 50f, true, arcPaint);

        Bitmap arrow = (this.connected ? this.arrowGreen : this.arrowBlack).getBitmap();
        float centerX = (getPaddingLeft() + getWidth()) / 2;
        float centerY = (getPaddingTop() + getHeight()) / 2;
        float leftArrow = centerX - (arrow.getWidth() / 2);
        float topArrow = centerY - (arrow.getHeight() / 2);
        canvas.drawBitmap(arrow, leftArrow, topArrow, this.arrowPaint);

        if (this.connected && this.otherLocation != null) {
            Bitmap markerBitmap = this.otherMarker.getBitmap();
            float distance = this.myLocation.distanceTo(this.otherLocation); // in
                                                                             // metres
            if (distance < 100.0f) {
                // This is an absolute bearing, relative to perfect north.
                double bearing = Math.toRadians(this.myLocation.bearingTo(this.otherLocation));
                double cosine = Math.cos(bearing - this.azimuth);
                double sine = Math.sin(bearing - this.azimuth);
                double drawDistance = centerX * (1 - Math.exp(-(SCALING * distance)));
                double expx = centerX + drawDistance * cosine - (markerBitmap.getWidth() / 2);
                double expy = centerY + drawDistance * sine - (markerBitmap.getHeight() / 2);
                canvas.drawBitmap(this.otherMarker.getBitmap(), (float) expx, (float) expy, this.arrowPaint);
            }
        }
    }
}