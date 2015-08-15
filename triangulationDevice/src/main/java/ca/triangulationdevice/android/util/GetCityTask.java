package ca.triangulationdevice.android.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public abstract class GetCityTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "GetCityTask";

    Context context;
    double latitude;
    double longitude;

    public GetCityTask(Context context, Location location) {
        this.context = context;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
    }

    @Override
    protected String doInBackground(Void... params) {
        Log.d(TAG, String.format("Looking up location for (%.4f, %.4f)", latitude, longitude));
        String result = "";
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                result = address.getLocality() + ", " + address.getAdminArea();
                Log.d(TAG, "Lookup complete: " + result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected abstract void onPostExecute(String result);
}