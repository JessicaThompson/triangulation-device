package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.model.User;
import ca.triangulationdevice.android.ui.partial.TriangulationActivity;

public class LoginActivity extends TriangulationActivity {

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
                User current = new User(profile.getName(), "", "", LoginActivity.this.getDrawable(R.drawable.gosling));
                LoginActivity.this.application.userManager.logIn(current);
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

    public void login(View v) {
        String name = loginName.getText().toString().trim();
        User current = new User(name, "", "", this.getDrawable(R.drawable.gosling));
        this.application.userManager.logIn(current);
        startActivity(new Intent(this, BrowseUserActivity.class));
        this.finish();
    }
}