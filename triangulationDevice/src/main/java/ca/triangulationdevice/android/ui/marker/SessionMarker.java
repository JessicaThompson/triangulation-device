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
import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.model.User;

public class SessionMarker extends Marker {
    public SessionMarker(Session session, Resources resources) {
        super(session.title, session.description, new LatLng(session.startLocation));
        this.setMarker(resources.getDrawable(R.drawable.map_archive_icon));
    }

    public SessionMarker(MapView mv, Session session, Resources resources) {
        super(mv, session.title, session.description, new LatLng(session.startLocation));
        this.setMarker(resources.getDrawable(R.drawable.map_archive_icon));
    }
}
