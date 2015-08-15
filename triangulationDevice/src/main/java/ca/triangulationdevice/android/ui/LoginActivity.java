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
import ca.triangulationdevice.android.util.GetCityTask;
import ca.triangulationdevice.android.util.NetworkUtils;

public class LoginActivity extends LocationActivity {

    private static final String TAG = "LoginActivity";

    private TextView loginName;
    private CallbackManager callbackManager;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.login);

        loginName = (TextView) findViewById(R.id.name_field);

        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton) findViewById(R.id.facebook_login);
        loginButton.setReadPermissions("public_profile");

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.i(TAG, "Successful login.");
                Profile profile = Profile.getCurrentProfile();
                final User current = new User();
                current.id = application.installation;
                current.name = profile.getName();
                current.myLocation = getLocation();
                current.picture = null;
                current.ip = NetworkUtils.getIPAddress(true);
                GetCityTask task = new GetCityTask(LoginActivity.this, current.myLocation) {
                    @Override
                    protected void onPostExecute(String result) {
                        current.location = result;
                        try {
                            application.userManager.add(current);

                            if (application.userManager.logIn(application.installation)) {
                                startActivity(new Intent(LoginActivity.this, BrowseUserActivity.class));
                                LoginActivity.this.finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Could not log in current user!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (CouchbaseLiteException e) {
                            Toast.makeText(LoginActivity.this, "Could not log in current user!", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                };
                task.execute();
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "Cancelled login.");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.i(TAG, "Error in login." + exception.getMessage());
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
            } else {
                Log.i(TAG, "Tried to log in but couldn't.");
            }
        } catch (CouchbaseLiteException ex) {
            // No user found.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void login(View v) {
        String name = loginName.getText().toString().trim();
        final User current = new User();
        current.id = application.installation;
        current.name = name;
        current.online = true;
        current.myLocation = getLocation();
        current.ip = NetworkUtils.getIPAddress(true);
        GetCityTask task = new GetCityTask(this, current.myLocation) {
            @Override
            protected void onPostExecute(String result) {
                current.location = result;
                try {
                    application.userManager.add(current);

                    if (application.userManager.logIn(application.installation)) {
                        startActivity(new Intent(LoginActivity.this, BrowseUserActivity.class));
                        LoginActivity.this.finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Could not log in current user!", Toast.LENGTH_SHORT).show();
                    }
                } catch (CouchbaseLiteException e) {
                    Toast.makeText(LoginActivity.this, "Could not log in current user!", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        };
        task.execute();
    }
}