package co.floxx.floxx;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;

public class MeetupPortalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetup_portal);

        Firebase.setAndroidContext(this);
        final Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        final String uid = ref.getAuth().getUid().toString();

        final ArrayList<String> confirmed = new ArrayList<String>();
        Intent intent = getIntent();
        for (String ouid : intent.getExtras().keySet()) {
            if (ouid != "meetup id") {
                confirmed.add(ouid);
            }
        }

        final String meetupId = intent.getStringExtra("meetup id");

        Button b = new Button(this);
        b.setText("Meetup " + meetupId.substring(meetupId.length() - 10));

        LinearLayout ll = (LinearLayout) findViewById(R.id.meetup_container);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ll.addView(b, lp);

        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MeetupPortalActivity.this, MapActivity.class);
                for (String ouid : confirmed) {
                    if (!ouid.equals(uid)) {
                        intent.putExtra(ouid, 0);
                    }
                }

                intent.putExtra("meetup id", meetupId);
                startActivity(intent);
            }
        });

        Button leaveButton = (Button) findViewById(R.id.leave_button);
        leaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                leave(meetupId, ref, uid);
                startActivity(new Intent(MeetupPortalActivity.this, FriendListActivity.class));
            }
        });

        FriendListActivity.initializeNames();
    }

    /**
     * Leaves the meetup. Enough said...
     */
    private void leave(final String meetupId, final Firebase ref, final String uid) {
        ref.child("ongoing").child(uid).setValue(null);

        Query meetupsRef = ref.child("meetups").child(meetupId).child("confirmed");
        meetupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<String> confirmed = (ArrayList<String>) snapshot.getValue();
                if (confirmed != null) {
                    confirmed.remove(uid);
                }

                ref.child("meetups").child(meetupId).child("confirmed").setValue(confirmed);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[MeetupPortalActivity] Error: " + firebaseError.getMessage());
            }
        });
    }
}
