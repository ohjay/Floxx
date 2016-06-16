package co.floxx.floxx;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;


public class FriendListActivity extends AppCompatActivity {
    FriendListActivity thisList;
    static Firebase ref;
    String uid; // the ID for the user in control of the device
    private HashSet<String> selected = new HashSet<String>();
    public static HashMap<String, String> names = new HashMap<String, String>();
    private HashSet<String> meetupParticipants = new HashSet<String>();
    private boolean participantsUpdated;
    private static final int GOLD = Color.parseColor("#ffde00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisList = this;
        setContentView(R.layout.activity_activity_friend_list);
        Button button = (Button) findViewById(R.id.sign_out_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FriendListActivity.this, FullscreenActivity.class));
            }
        });

        ImageButton button1 = (ImageButton) findViewById(R.id.setting_logo);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FriendListActivity.this, SettingActivity.class));
            }
        });

        names.put(FirebaseActivity.OSKI_UID, "oski"); // just to be comprehensive

        // TODO: find some way to clear the SELECTED set every time the view loads

        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");
        uid = ref.getAuth().getUid().toString();

        initializeNames();
        updateMeetupParticipants();

        Query queryRef = ref.child("users").orderByKey().equalTo(uid);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Object result = dataSnapshot.child(uid).child("friends").getValue();
                if (result != null) {
                    ArrayList<String> friends = (ArrayList<String>) result;
                    blockForParticipants();

                    for (final String fuid : friends) {
                        if (meetupParticipants.contains(fuid)) {
                            continue; // rule #1: users can only be in one meetup at once
                        }

                        final Button b = new Button(thisList);
                        Query nameQRef = ref.child("uids").orderByValue().equalTo(fuid);
                        nameQRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot child : dataSnapshot.getChildren()) {
                                    b.setText(child.getKey());

                                    LinearLayout ll = (LinearLayout) findViewById(R.id.button_container);
                                    LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                                            LayoutParams.WRAP_CONTENT);
                                    ll.addView(b, lp);

                                    b.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View view) {
                                            if (selected.contains(fuid)) {
                                                selected.remove(fuid);
                                                b.getBackground().clearColorFilter();
                                            } else {
                                                selected.add(fuid);
                                                b.getBackground().setColorFilter(GOLD,
                                                        PorterDuff.Mode.DARKEN);
                                            }
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {}
                        });
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });

        // Add meetup invitations to the layout
        Query inviteRef = ref.child("invitations");
        inviteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> invitations =
                        (ArrayList<String>) dataSnapshot.child(uid).getValue();

                if (invitations != null) {
                    for (final String meetupId : invitations) {
                        String iuid = meetupId.substring(0, meetupId.length() - 10);
                        Button b = new Button(thisList);
                        b.setText(names.get(iuid) + "'s meetup");

                        LinearLayout ll = (LinearLayout) findViewById(R.id.meetup_container);
                        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT);
                        ll.addView(b, lp);

                        b.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View view) {
                                acceptMeetupInvitation(meetupId);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println(firebaseError.getMessage());
            }
        });

        Button mapButton = (Button) findViewById(R.id.mapgo);
        mapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View view) {
                if (meetupParticipants.contains(uid)) {
                    Toast.makeText(FriendListActivity.this, "User is already in a meetup!",
                            Toast.LENGTH_LONG).show();
                } else {
                    switchToMap(view);
                }
            }
        });
    }

    private void acceptMeetupInvitation(final String meetupId) {
        // Remove meetup invitations for the current user
        ref.child("invitations").child(uid).setValue(null);

        // Add to "ongoing" and "meetups" sections of the database
        final ArrayList<String> confirmed = new ArrayList<String>();
        ref.child("ongoing").child(uid).setValue(meetupId);
        Query meetupsRef = ref.child("meetups").child(meetupId).child("confirmed");
        meetupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<String> sconfirmed = (ArrayList<String>) snapshot.getValue();
                if (sconfirmed != null) { // and it SHOULDN'T be null but...
                    confirmed.addAll(sconfirmed);
                }

                confirmed.add(uid);
                ref.child("meetups").child(meetupId).child("confirmed").setValue(confirmed);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println(firebaseError.getMessage());
            }
        });

        Intent intent = new Intent(FriendListActivity.this, MapActivity.class);
        for (String ouid : confirmed) {
            if (!ouid.equals(uid)) {
                intent.putExtra(ouid, 0);
            }
        }
        intent.putExtra("meetup id", meetupId);
        startActivity(intent);
    }

    /**
     * Updates the set of all users who are currently in a meetup.
     */
    private void updateMeetupParticipants() {
        meetupParticipants.clear();
        participantsUpdated = false;
        Query ongoingRef = ref.child("ongoing");
        ongoingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot c : snapshot.getChildren()) {
                    meetupParticipants.add(c.getKey());
                }
                participantsUpdated = true;
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    /**
     * Blocks until meetup participants have been identified.
     */
    private void blockForParticipants() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (participantsUpdated) {
                    return;
                } else {
                    blockForParticipants();
                }
            }
        }, 350);
    }

    /**
     * Assumes that the user is STARTING the meetup
     * (i.e. no other users have had time to confirm their invitations yet).
     */
    public void switchToMap(View view) {
        Intent intent = new Intent(FriendListActivity.this, MapActivity.class);

        // Add to the meetups section of the database
        ArrayList<String> confirmed = new ArrayList<String>(); // confirmed users
        confirmed.add(uid);
        final String date = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
        ref.child("meetups").child(uid + date).child("confirmed").setValue(confirmed);

        // Add the meetup to the user's ongoing meetup list
        ref.child("ongoing").child(uid).setValue(uid + date);

        // Send out meetup invitations
        for (final String ouid : selected) {
            Query inviteRef = ref.child("invitations");
            inviteRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    ArrayList<String> invitations =
                            (ArrayList<String>) snapshot.child(ouid).getValue();

                    if (invitations == null) {
                        invitations = new ArrayList<String>();
                    }

                    invitations.add(uid + date);
                    ref.child("invitations").child(ouid).setValue(invitations);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    System.out.println(firebaseError.getMessage());
                }
            });
        }

        intent.putExtra("meetup id", uid + date);
        FriendListActivity.this.startActivity(intent);
    }

    public static void initializeNames() {
        ref = new Firebase("https://floxx.firebaseio.com/");
        ref.child("uids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    names.put(child.getValue().toString(), child.getKey());
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
                System.out.println("[FriendListActivity] Read error: " + error.getMessage());
            }
        });
    }
}
