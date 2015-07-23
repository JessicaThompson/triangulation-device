package ca.triangulationdevice.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.facebook.FacebookSdk;
import com.facebook.login.widget.LoginButton;

import ca.triangulationdevice.android.R;
import ca.triangulationdevice.android.ui.partial.TriangulationActivity;

public class LoginActivity extends TriangulationActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.login);

        LoginButton loginButton = (LoginButton) findViewById(R.id.facebook_login);
        loginButton.setReadPermissions("public_profile");
    }

    public void login(View v) {
        startActivity(new Intent(this, BrowseUserActivity.class));
    }
}