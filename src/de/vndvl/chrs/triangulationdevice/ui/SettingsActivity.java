package de.vndvl.chrs.triangulationdevice.ui;

import android.os.Bundle;
import android.view.Menu;
import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.ui.partial.TriangulationPreferenceActivity;

public class SettingsActivity extends TriangulationPreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_settings).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }
}
