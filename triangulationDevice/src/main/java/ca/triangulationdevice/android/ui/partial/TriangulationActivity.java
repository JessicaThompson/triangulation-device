package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.TriangulationApplication;

/**
 * An {@link Activity} which controls the action bar menu stuff for us.
 */
public abstract class TriangulationActivity extends Activity {

    protected TriangulationApplication application;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        application = (TriangulationApplication) getApplication();
    }
}
