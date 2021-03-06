package ca.triangulationdevice.android.util;

import android.content.Context;
import android.graphics.Typeface;

/**
 * A convenience class to load our typefaces when this class is imported, and
 * making them accessible to whoever.
 */
public class Typefaces {
    public static boolean initialized = false;

    public static Typeface raleway;
    public static Typeface ralewaySemiBold;
    public static Typeface ralewayBold;
    public static Typeface ralewayLight;

    public static void loadTypefaces(Context context) {
        if (!initialized) {
            raleway = Typeface.createFromAsset(context.getAssets(), "fonts/Raleway-Regular.otf");
            ralewaySemiBold = Typeface.createFromAsset(context.getAssets(), "fonts/Raleway-SemiBold.otf");
            ralewayBold = Typeface.createFromAsset(context.getAssets(), "fonts/Raleway-Bold.otf");
            ralewayLight = Typeface.createFromAsset(context.getAssets(), "fonts/Raleway-Light.otf");
            initialized = true;
        }
    }
}
