package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.os.Bundle;
import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.ui.partial.TriangulationActivity;

/**
 * An activity which shows an "About this App" screen to the user.
 */
public class TextActivity extends TriangulationActivity {

    public static final String LAYOUT_EXTRA_KEY = "layout";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent loadingIntent = this.getIntent();
        setContentView(loadingIntent.getIntExtra(LAYOUT_EXTRA_KEY, R.layout.credits));
    }
}
