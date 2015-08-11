package ca.triangulationdevice.android.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.ui.partial.TriangulationActivity;

public class EditProfileActivity extends TriangulationActivity {

    public final static String EXTRA_USER_ID = "userid";
    private User user;
    private EditText nameView;
    private EditText descriptionView;
    private EditText locationView;
    private EditText emailView;
    private ImageView profileImage;
    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.edit_profile);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent callingIntent = getIntent();
        String userId = callingIntent.getStringExtra(ProfileActivity.EXTRA_USER_ID);
        user = application.userManager.getUser(userId);

        profileImage = (ImageView) findViewById(R.id.profile_image);
        nameView = (EditText) findViewById(R.id.edit_name);
        descriptionView = (EditText) findViewById(R.id.edit_bio);
        locationView = (EditText) findViewById(R.id.edit_location);
        emailView = (EditText) findViewById(R.id.edit_email);

        profileImage.setImageDrawable(user.picture);

        nameView.setText(user.name);
        descriptionView.setText(user.description);
        locationView.setText(user.location);
        emailView.setText(user.email);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_title);
        builder.setTitle(R.string.unsaved_description);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                EditProfileActivity.this.onBackPressed();
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        dialog = builder.create();
    }

    public void save() {
        user.name = nameView.getText().toString().trim();
        user.description = descriptionView.getText().toString();
        user.email = emailView.getText().toString();
        user.location = locationView.getText().toString();

        application.userManager.add(user);
    }

    public void logout(View v) {
        application.userManager.logOut();
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    public void onBackPressed() {
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_profile_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_save:
                this.save();
                this.finish();
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
