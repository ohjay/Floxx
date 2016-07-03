package co.floxx.floxx;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;

public class ConfirmationActivity extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intermediary.confToFullscreen = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        Intent intent = getIntent();
        final String vcode = intent.getStringExtra("vcode");
        final String username = intent.getStringExtra("username");
        final EditText confEntry = (EditText) findViewById(R.id.conf_entry);

        Button submitButton = (Button) findViewById(R.id.conf_submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String entry = confEntry.getText().toString();
                if (entry.toUpperCase().equals(vcode.toUpperCase())) {
                    // All right! Confirmed :)
                    Firebase ref = new Firebase("https://floxx.firebaseio.com/");
                    AuthData auth = ref.getAuth();
                    if (auth == null) {
                        // This shouldn't be happening
                        Toast.makeText(ConfirmationActivity.this,
                                "Interesting. Sorry, but can you "
                                + "log out and log in again?",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = auth.getUid();
                    ref.child("status " + uid).setValue("active");
                    startUserPortal(username);
                } else {
                    // Log.wtf man...
                    String msg = "Sorry, that wasn't right. Check your email again for the code!";
                    Toast.makeText(ConfirmationActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });

        Button signOutButton = (Button) findViewById(R.id.conf_out);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intermediary.confToFullscreen = true;
                new Firebase("https://floxx.firebaseio.com/").unauth();
                finish();
            }
        });
    }

    private void startUserPortal(final String username) {
        Intent intent = new Intent(ConfirmationActivity.this, UserPortalActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }
}
