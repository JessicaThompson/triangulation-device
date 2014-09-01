package de.vndvl.chrs.triangulationdevice.ui.partial;

import de.vndvl.chrs.triangulationdevice.R;
import de.vndvl.chrs.triangulationdevice.ui.AboutActivity;
import de.vndvl.chrs.triangulationdevice.ui.ArchiveActivity;
import de.vndvl.chrs.triangulationdevice.ui.SettingsActivity;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

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