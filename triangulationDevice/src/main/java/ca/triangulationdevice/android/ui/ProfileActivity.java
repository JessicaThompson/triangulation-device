package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;

import java.util.Collections;
import java.util.List;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.model.Session;
import ca.triangulationdevice.android.model.SessionAdapter;
import ca.triangulationdevice.android.ui.partial.TriangulationListActivity;

public class ProfileActivity extends TriangulationListActivity {

    private final static String TAG = "ProfileActivity";

    public final static String EXTRA_USER_ID = "userid";
    private User user;
    private ImageView profileImage;
    private TextView nameView;
    private TextView descriptionView;
    private TextView locationView;
    private Button connect;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.profile);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        profileImage = (ImageView) findViewById(R.id.profile_image);
        nameView = (TextView) findViewById(R.id.name);
        descriptionView = (TextView) findViewById(R.id.description);
        locationView = (TextView) findViewById(R.id.location);

        // If we're looking at our own profile, hide the
        connect = (Button) findViewById(R.id.connect);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent callingIntent = getIntent();
        String userId = callingIntent.getStringExtra(ProfileActivity.EXTRA_USER_ID);
        try {
            user = application.userManager.getUser(userId);
        } catch (CouchbaseLiteException ex) {
            Log.e(TAG, "Error loading user from database.");
            Toast.makeText(this, "Error loading user!", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        if (!user.name.isEmpty()) {
            nameView.setText(user.name);
        } else {
            nameView.setText(R.string.no_name);
        }

        if (!user.description.isEmpty()) {
            descriptionView.setText(user.description);
        } else {
            descriptionView.setText(R.string.no_description);
        }

        if (!user.location.isEmpty()) {
            locationView.setText(user.location);
        } else {
            locationView.setText(R.string.no_location);
        }
        profileImage.setImageDrawable(user.picture);

        if (user == application.userManager.getCurrentUser()) {
            connect.setVisibility(View.GONE);
        } else if (!user.online) {
            connect.setEnabled(false);
        }

        List<Session> sessions;
        try {
            sessions = application.userManager.getSessionsForUser(user);
        } catch (CouchbaseLiteException ex) {
            sessions = Collections.emptyList();
        }
        getListView().setAdapter(new SessionAdapter(this, sessions));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_edit:
                Intent intent = new Intent(this, EditProfileActivity.class);
                intent.putExtra(EditProfileActivity.EXTRA_USER_ID, user.id);
                startActivity(intent);
                return true;
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void connect(View v) {
        if (user.online)
            startActivity(new Intent(this, RecordWalkActivity.class));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
    }
}