package ca.triangulationdevice.android;

import android.app.Application;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.mapbox.mapboxsdk.geometry.LatLng;

import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.model.MemoryUserManager;

public class TriangulationApplication extends Application {
    public MemoryUserManager userManager;

    @Override
    public void onCreate() {
        super.onCreate();
            userManager = new MemoryUserManager();

            Drawable ryanDrawable = getResources().getDrawable(R.drawable.gosling);
            User ryanGosling = new User("Ryan Gosling", "Canadian actor, film director, screenwriter, musician and businessman.", "Toronto, ON", ryanDrawable);
            ryanGosling.myLocation = new Location("made up");
            ryanGosling.myLocation.setLatitude(43.454748);
            ryanGosling.myLocation.setLongitude(-80.549960);
            this.userManager.add(ryanGosling);

            User rachelMcAdams = new User("Rachel McAdams", "I'm curious to live. Curiosity and moves me forward", "London, ON");
            rachelMcAdams.myLocation = new Location("made up");
            rachelMcAdams.myLocation.setLatitude(43.453748);
            rachelMcAdams.myLocation.setLongitude(-80.543960);
            this.userManager.add(rachelMcAdams);
        }

}
