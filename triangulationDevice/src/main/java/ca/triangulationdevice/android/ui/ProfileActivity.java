package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.storage.PathStorage;
import ca.triangulationdevice.android.storage.SessionAdapter;
import ca.triangulationdevice.android.ui.partial.TriangulationListActivity;

public class ProfileActivity extends TriangulationListActivity {

    public final static String EXTRA_USER_ID = "userid";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.profile);

        Intent callingIntent = getIntent();
        String userId = callingIntent.getStringExtra(ProfileActivity.EXTRA_USER_ID);
        User user = application.userManager.getUser(userId);

        ImageView profileImage = (ImageView) findViewById(R.id.profile_image);
        TextView nameView = (TextView) findViewById(R.id.name);
        TextView descriptionView = (TextView) findViewById(R.id.description);
        TextView locationView = (TextView) findViewById(R.id.location);

        nameView.setText(user.name);
        descriptionView.setText(user.description);
        locationView.setText(user.location);
        profileImage.setImageDrawable(user.picture);

        List<PathStorage.Session> sessions = application.userManager.getSessionsForUser(user);
        getListView().setAdapter(new SessionAdapter(this, sessions));
    }

    public void connect(View v) {
        startActivity(new Intent(this, RecordWalkActivity.class));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
    }
}