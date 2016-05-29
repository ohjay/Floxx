package co.floxx.floxx;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.firebase.client.Firebase;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Firebase.setAndroidContext(this);
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_fullscreen);
        EditText password =(EditText)findViewById(R.id.enter_password);
        final EditText password1 = (EditText) findViewById(R.id.enter_password);

        // Cool fonts let's go!
        Typeface montserrat = Typeface.createFromAsset(getAssets(), "Montserrat-Regular.otf");
        EditText unameText = (EditText) findViewById(R.id.enter_username);
        EditText pwText = (EditText) findViewById(R.id.enter_password);
        unameText.setTypeface(montserrat);
        pwText.setTypeface(montserrat);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        Firebase.setAndroidContext(this);
        Button button = (Button)findViewById(R.id.log_in_button);
        final Bundle extras = new Bundle();
        final EditText username1 = (EditText) findViewById(R.id.enter_username);
        // sendingSetting();
        registerAccount();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FullscreenActivity.this, FirebaseActivity.class);
                String username11 = username1.getText().toString();
                String password11 = password1.getText().toString();
                extras.putString("enter_username", username11);
                extras.putString("enter_password", password11);
                intent.putExtras(extras);
                startActivity(intent);
            }
        });

    }

    public void sendingSetting() {
        ImageButton button = (ImageButton) findViewById(R.id.setting_logo);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FullscreenActivity.this, SettingActivity.class));
            }
        });
    }

    public void registerAccount() {
        TextView button = (TextView) findViewById(R.id.register_button);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FullscreenActivity.this, RegisterActivity.class));
            }
        });
    }

    public void switchToFriendList(View view) {
        Intent intent = new Intent(FullscreenActivity.this, ActivityFriendList.class);
        FullscreenActivity.this.startActivity(intent);
    }
}
