package ca.triangulationdevice.android.ui.marker;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;

public class UserMarker extends Marker {
    private static final String TAG = "UserMarker";

    public UserMarker(User user, Resources resources) {
        super(user.name, user.description, new LatLng(user.myLocation));
        this.setPicture(user, resources);
    }

    public UserMarker(MapView mv, User user, Resources resources) {
        super(mv, user.name, user.description, new LatLng(user.myLocation));
        setPicture(user, resources);
    }

    private void setPicture(User user, Resources resources) {
        if (user.picture == null) {
            this.setMarker(resources.getDrawable(R.drawable.map_archive_icon));
        } else {
            this.setMarker(new BitmapDrawable(resources, circularize(user.picture)));
            this.setHotspot(HotspotPlace.CENTER);
        }
    }

    private Bitmap circularize(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
}
