package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.ui.partial.TriangulationListActivity;

/**
 * An activity which shows an "About this App" screen to the user.
 */
public class AboutActivity extends TriangulationListActivity {

    // Wish I could get this from the app..
    private final static int WEBSITE = 0;
    private final static int RATE_REVIEW = 1;
    private final static int HOW_TO_USE = 2;
    // private final static int PRIVACY = 3;
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
        case HOW_TO_USE:
            Intent howIntent = new Intent(this, TextActivity.class);
            howIntent.putExtra(TextActivity.LAYOUT_EXTRA_KEY, R.layout.how_to_use);
            startActivity(howIntent);
            break;
        // case PRIVACY:
        // Intent privacyIntent = new Intent(this, TextActivity.class);
        // privacyIntent.putExtra(TextActivity.LAYOUT_EXTRA_KEY,
        // R.layout.privacy);
        // startActivity(privacyIntent);
        // break;
        case CREDITS:
            Intent creditsIntent = new Intent(this, TextActivity.class);
            creditsIntent.putExtra(TextActivity.LAYOUT_EXTRA_KEY, R.layout.credits);
            startActivity(creditsIntent);
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
