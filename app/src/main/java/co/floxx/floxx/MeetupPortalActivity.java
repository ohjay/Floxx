package co.floxx.floxx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;

public class MeetupPortalActivity extends AppCompatActivity {
    BroadcastReceiver receiver;

    @Override
    protected void onResume() {
        super.onResume();

        Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    finish(); // no meetup to be found, so we're done here
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[MPA – onResume] Read failed: " + firebaseError.getMessage());
            }
        };

        ref.child("ongoing").child(ref.getAuth().getUid()).addListenerForSingleValueEvent(vel);
    }

    @Override
    public void onBackPressed() {
        // Log this fool out
        super.onBackPressed();
        new Firebase("https://floxx.firebaseio.com/").unauth();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetup_portal);

        Firebase.setAndroidContext(this);
        final Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        final String uid = ref.getAuth().getUid();

        final ArrayList<String> confirmed = new ArrayList<String>();
        Intent intent = getIntent();
        for (String ouid : intent.getExtras().keySet()) {
            if (!ouid.equals("meetup id")) {
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
                Utility.leave(meetupId, ref, uid);
                startActivity(new Intent(MeetupPortalActivity.this, FriendListActivity.class));
            }
        });

        ImageButton settingButton = (ImageButton) findViewById(R.id.setting_button);
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MeetupPortalActivity.this, SettingsActivity.class));
            }
        });

        FriendListActivity.initializeNames();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("co.floxx.floxx.ACTION_LOGOUT");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startActivity(new Intent(MeetupPortalActivity.this, FullscreenActivity.class));
                finish();
            }
        };
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }
}
