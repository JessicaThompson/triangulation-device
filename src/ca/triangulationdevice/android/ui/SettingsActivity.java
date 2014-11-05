package ca.triangulationdevice.android.ui;

import android.os.Bundle;
import android.view.Menu;
import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.ui.partial.TriangulationPreferenceActivity;

public class SettingsActivity extends TriangulationPreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean displayed = super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_settings).setVisible(false);
        return displayed;
    }
}
