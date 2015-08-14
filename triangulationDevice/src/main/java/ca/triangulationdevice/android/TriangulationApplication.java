package ca.triangulationdevice.android;

import android.app.Application;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;

import ca.triangulationdevice.android.storage.CouchDBUserManager;
import ca.triangulationdevice.android.util.Installation;

public class TriangulationApplication extends Application {
    public CouchDBUserManager userManager;
    public String installation;

    @Override
    public void onCreate() {
        super.onCreate();
        installation = Installation.id(this);
        userManager = new CouchDBUserManager(getApplicationContext());
    }

    public void onDestroy() {
//        userManager.getCurrentUser().online = false;
//        try {
//            userManager.add(userManager.getCurrentUser());
//        } catch (CouchbaseLiteException ex) {
//            Log.e("TriangulationApp", ex.getMessage());
//        }
    }

}
