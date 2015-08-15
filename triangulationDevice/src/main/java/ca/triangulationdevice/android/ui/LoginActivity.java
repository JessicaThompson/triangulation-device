package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.ui.partial.LocationActivity;
import ca.triangulationdevice.android.util.NetworkUtils;

public class LoginActivity extends LocationActivity {

    private static final String TAG = "LoginActivity";

    private TextView loginName;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.login);

        loginName = (TextView) findViewById(R.id.name_field);

        final CallbackManager callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton) findViewById(R.id.facebook_login);
        loginButton.setReadPermissions("public_profile");

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(LoginActivity.this, "Success!", Toast.LENGTH_LONG).show();
                Profile profile = Profile.getCurrentProfile();
                User current = new User();
                current.id = application.installation;
                current.name = profile.getName();
                current.myLocation = getLocation();
                current.picture = null;
                startActivity(new Intent(LoginActivity.this, BrowseUserActivity.class));
                LoginActivity.this.finish();
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(FacebookException exception) {
                Toast.makeText(LoginActivity.this, "An exception occurred - please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            // If we can log in, redirect.
            if (this.application.userManager.logIn(application.installation)) {
                startActivity(new Intent(this, BrowseUserActivity.class));
                this.finish();
            }
        } catch (CouchbaseLiteException ex) {
            // No user found.
        }
    }

    public void login(View v) {
        String name = loginName.getText().toString().trim();
        User current = new User();
        current.id = application.installation;
        current.name = name;
        current.online = true;
        current.myLocation = getLocation();
        current.ip = NetworkUtils.getIPAddress(true);
        try {
            this.application.userManager.add(current);
            if (this.application.userManager.logIn(application.installation)) {
                startActivity(new Intent(this, BrowseUserActivity.class));
                this.finish();
            } else {
                Toast.makeText(this, "Could not log in current user!", Toast.LENGTH_SHORT).show();
            }
        } catch (CouchbaseLiteException ex) {
            Toast.makeText(this, "Could not log in current user!", Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
        }
    }
}