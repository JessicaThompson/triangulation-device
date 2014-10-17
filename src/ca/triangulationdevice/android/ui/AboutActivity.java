package ca.triangulationdevice.android.ui;

import ca.triangulationdevice.android.ui.partial.TriangulationListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.vndvl.chrs.triangulationdevice.R;

/**
 * An activity which shows an "About this App" screen to the user.
 */
public class AboutActivity extends TriangulationListActivity {

    // Wish I could get this from the app..
    private final static int WEBSITE = 0;
    private final static int RATE_REVIEW = 1;
    private final static int PRIVACY = 2;
    private final static int CREDITS = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        // Set our ListView.
        String[] aboutMenu = getResources().getStringArray(R.array.about_sections);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, aboutMenu);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
        case WEBSITE:
            Uri webpage = Uri.parse(getResources().getString(R.string.website));
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
            break;
        case RATE_REVIEW:
            final String appPackageName = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
            }
            break;
        case PRIVACY:
            // TODO: Launch privacy policy.
            break;
        case CREDITS:
            // TODO: Launch credits.
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean displayed = super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_about).setVisible(false);
        return displayed;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}
