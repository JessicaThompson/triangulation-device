package ca.triangulationdevice.android;

import android.app.Application;
import android.graphics.drawable.Drawable;

import com.mapbox.mapboxsdk.geometry.LatLng;

import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.model.UserManager;

public class TriangulationApplication extends Application {
    public UserManager userManager = new UserManager();

    @Override
    public void onCreate() {
        super.onCreate();

        LatLng ryanPos = new LatLng(43.454748, -80.549960);
        Drawable ryanDrawable = getResources().getDrawable(R.drawable.gosling);
        User ryanGosling = new User("Ryan Gosling", "Canadian actor, film director, screenwriter, musician and businessman.", "Toronto, ON", ryanPos, ryanDrawable);
        this.userManager.add(ryanGosling);

        LatLng rachelPos = new LatLng(43.453748, -80.543960);
        User rachelMcAdams = new User("Rachel McAdams", "I'm curious to live. Curiosity and moves me forward", "London, ON", rachelPos);
        this.userManager.add(rachelMcAdams);
    }
}
