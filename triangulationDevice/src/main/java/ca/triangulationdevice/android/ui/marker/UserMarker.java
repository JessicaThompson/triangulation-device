package ca.triangulationdevice.android.ui.marker;

import android.content.res.Resources;
import android.graphics.Bitmap;
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
            Drawable scaled = new BitmapDrawable(Bitmap.createScaledBitmap(user.picture, 320, 320, true));
            this.setMarker(scaled);
        }
    }
}
