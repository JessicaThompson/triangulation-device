package ca.triangulationdevice.android.ui.partial;

import ca.triangulationdevice.android.ui.AboutActivity;
import ca.triangulationdevice.android.ui.ArchiveActivity;
import ca.triangulationdevice.android.ui.SettingsActivity;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import de.vndvl.chrs.triangulationdevice.R;

/**
 * An {@link Activity} which controls the action bar menu stuff for us.
 */
public abstract class TriangulationActivity extends Activity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nav_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
        case R.id.menu_about:
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        case R.id.menu_archive:
            startActivity(new Intent(this, ArchiveActivity.class));
            return true;
        case R.id.menu_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
