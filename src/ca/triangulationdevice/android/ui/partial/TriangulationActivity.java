package ca.triangulationdevice.android.ui.partial;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.ui.ArchiveActivity;

/**
 * An {@link Activity} which controls the action bar menu stuff for us.
 */
public abstract class TriangulationActivity extends Activity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nav_menu, menu);
        menu.findItem(R.id.menu_settings).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
        case R.id.menu_archive:
            startActivity(new Intent(this, ArchiveActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
