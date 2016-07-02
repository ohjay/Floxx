package co.floxx.floxx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.AuthData;
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
        AuthData auth = ref.getAuth();
        if (auth == null) {
            // Not supposed to be here!
            // Possibly MP1 -> FL -> map -> (X map) -> (X FL) -> MP2 -> (X MP2) = MP1
            // ^ Update (7/02): this is outdated but it looks cool so I'm keeping it

            finish(); return; // get the heck back to the login screen
        }

        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    startActivity(new Intent(MeetupPortalActivity.this,
                            FriendListActivity.class));
                    finish(); // no meetup to be found, so we're done here
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("MPA – onResume", "Read failed: " + firebaseError.getMessage());
            }
        };

        ref.child("ongoing").child(auth.getUid()).addListenerForSingleValueEvent(vel);
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

        Typeface montserrat = Typeface.createFromAsset(getAssets(), "Montserrat-Regular.otf");
        ((TextView) findViewById(R.id.am_header)).setTypeface(montserrat);

        final String meetupId = intent.getStringExtra("meetup id");

        int dBegin = meetupId.length() - 19, tBegin = meetupId.length() - 8;
        String date = meetupId.substring(dBegin, tBegin - 1);
        String time = meetupId.substring(tBegin);

        Button b = new Button(this);
        String meetupInfo = date + " meetup\n[Started @ " + time + "]";
        b.setText(meetupInfo);

        TextView bDesc = new TextView(this);
        bDesc.setTextColor(Color.LTGRAY);

        // Create the participants information string
        String participantsInfo = "Current participants: <b>you</b>";
        int numConfirmed = confirmed.size();
        if (numConfirmed > 0) { participantsInfo += ","; }

        for (int i = 0; i < numConfirmed; ++i) {
            String participantName = FriendListActivity.names.get(confirmed.get(i));
            participantsInfo += " " + ((participantName != null) ? participantName : "???");

            if (i != numConfirmed - 1) {
                participantsInfo += ",";
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            bDesc.setText(Html.fromHtml(participantsInfo, Html.FROM_HTML_MODE_LEGACY));
        } else {
            bDesc.setText(Html.fromHtml(participantsInfo));
        }

        LinearLayout ll = (LinearLayout) findViewById(R.id.meetup_container);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ll.addView(b, lp);
        ll.addView(bDesc, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

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
                finish();
            }
        });

        Button userPortalButton = (Button) findViewById(R.id.user_portal_mbutton);
        userPortalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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
