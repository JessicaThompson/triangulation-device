package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
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
    private String userId;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ListView listView = getListView();
        LayoutInflater inflater = getLayoutInflater();
        View header = inflater.inflate(R.layout.profile, listView, false);
        listView.addHeaderView(header);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent callingIntent = getIntent();
        userId = callingIntent.getStringExtra(ProfileActivity.EXTRA_USER_ID);

        profileImage = (ImageView) header.findViewById(R.id.profile_image);
        nameView = (TextView) header.findViewById(R.id.name);
        descriptionView = (TextView) header.findViewById(R.id.description);
        locationView = (TextView) header.findViewById(R.id.location);

        // If we're looking at our own profile, hide the connect button.
        connect = (Button) header.findViewById(R.id.connect);
    }

    @Override
    public void onResume() {
        super.onResume();

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

        if (user.picture != null) {
            profileImage.setImageBitmap(user.picture);
        }

        if (user.id.equals(application.userManager.getCurrentUser().id)) {
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
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void connect(View v) {
        if (user.online) {
            Intent intent = new Intent(this, RecordWalkActivity.class);
            intent.putExtra(RecordWalkActivity.ID_EXTRA, user.id);
            startActivity(intent);
            this.finish();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
    }
}