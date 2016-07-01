package co.floxx.floxx;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;

public class ConfirmationActivity extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new Firebase("https://floxx.firebaseio.com/").unauth();
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
                    String uid = ref.getAuth().getUid();
                    ref.child("status " + uid).setValue("active");
                    startUserPortal(ref, uid, username);
                } else {
                    // Log.wtf...
                    String msg = "Sorry, that wasn't right. Check your email again for the code!";
                    Toast.makeText(ConfirmationActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });

        Button signOutButton = (Button) findViewById(R.id.conf_out);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Firebase("https://floxx.firebaseio.com/").unauth();
                finish();
            }
        });
    }

    private void startUserPortal(final Firebase ref, final String uid, final String username) {
        Query ongoingRef = ref.child("ongoing");
        ongoingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.hasChild(uid)) {
                    // Signal that the user is NOT currently involved in a meetup
                    Intent intent = new Intent(ConfirmationActivity.this, UserPortalActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish(); return;
                }

                // Signal that the user is actually in a meetup already
                final String meetupId = snapshot.child(uid).getValue().toString();
                Query meetupsRef = ref.child("meetups").child(meetupId).child("confirmed");
                meetupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Intermediary.firebaseConfirmed = new ArrayList<String>(
                                (ArrayList<String>) snapshot.getValue());
                        Intermediary.firebaseMeetupId = meetupId;

                        Intent intent = new Intent(
                                ConfirmationActivity.this, UserPortalActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        Log.w("CA – sUP x2", "Read failed: " + firebaseError.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("CA – sUP", "Read failed: " + firebaseError.getMessage());
            }
        });
    }
}
