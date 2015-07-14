package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.ui.partial.TriangulationActivity;

public class ProfileActivity extends TriangulationActivity {

    public final static String EXTRA_USER_ID = "userid";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.profile);

        Intent callingIntent = getIntent();
        String userId = callingIntent.getStringExtra(ProfileActivity.EXTRA_USER_ID);
        User user = application.userManager.get(userId);

        ImageView profileImage = (ImageView) findViewById(R.id.profile_image);
        TextView nameView = (TextView) findViewById(R.id.name);
        TextView descriptionView = (TextView) findViewById(R.id.description);
        TextView locationView = (TextView) findViewById(R.id.location);

        nameView.setText(user.name);
        descriptionView.setText(user.description);
        locationView.setText(user.location);
        profileImage.setImageDrawable(user.picture);
    }
}