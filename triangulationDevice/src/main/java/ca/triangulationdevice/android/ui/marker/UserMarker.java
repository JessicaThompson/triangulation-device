package ca.triangulationdevice.android.ui.marker;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;

public class UserMarker extends Marker {
    public UserMarker(User user, Resources resources) {
        super(user.name, user.description, user.latLng);
        this.setPicture(user, resources);
    }

    public UserMarker(MapView mv, User user, Resources resources) {
        super(mv, user.name, user.description, user.latLng);
        setPicture(user, resources);
    }

    private void setPicture(User user, Resources resources) {
        if (user.picture == null) {
            this.setMarker(resources.getDrawable(R.drawable.map_archive_icon));
        } else {
            int width = user.picture.getIntrinsicWidth();
            width = width > 0 ? width : 1;
            int height = user.picture.getIntrinsicHeight();
            height = height > 0 ? height : 1;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            user.picture.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            user.picture.draw(canvas);

            Drawable scaled = new BitmapDrawable(Bitmap.createScaledBitmap(bitmap, 320, 320, true));
            this.setMarker(scaled);
        }
    }
}
