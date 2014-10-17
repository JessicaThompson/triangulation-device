package ca.triangulationdevice.android.ui;

import ca.triangulationdevice.android.ui.partial.TriangulationPreferenceActivity;
import android.os.Bundle;
import android.view.Menu;
import de.vndvl.chrs.triangulationdevice.R;

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
