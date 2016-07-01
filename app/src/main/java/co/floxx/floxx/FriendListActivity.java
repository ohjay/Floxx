package co.floxx.floxx;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private boolean recentCreation;
    private ArrayList<String> friends;

    @Override
    protected void onResume() {
        super.onResume();

        if (recentCreation) {
            recentCreation = false;
            return;
        } else if (Intermediary.mapToPortalMeetupId != null) {
            // The map wanted us to go to the portal. To the portal we shall go
            Intent intent = new Intent(this, MeetupPortalActivity.class);
            for (String ouid : Intermediary.mapToPortalOthers) {
                intent.putExtra(ouid, 0);
            }
            intent.putExtra("meetup id", Intermediary.mapToPortalMeetupId);

            Intermediary.mapToPortalOthers = null;
            Intermediary.mapToPortalMeetupId = null;

            startActivity(intent);
            finish(); // this is not the place to be
            return;
        }

        // Update the friend list (should also take care of deselection)
        ((LinearLayout) findViewById(R.id.button_container)).removeAllViews();
        resetFriendList();

        // Update the meetup invitation list
        ((LinearLayout) findViewById(R.id.meetup_container)).removeAllViews();
        resetMeetupInvitations();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisList = this;
        setContentView(R.layout.activity_friend_list);

        Typeface montserrat = Typeface.createFromAsset(getAssets(), "Montserrat-Regular.otf");
        ((TextView) findViewById(R.id.gc_header)).setTypeface(montserrat);
        ((TextView) findViewById(R.id.meetup_invitations)).setTypeface(montserrat);

        Button userPortalButton = (Button) findViewById(R.id.up_back);
        userPortalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        names.put(FirebaseActivity.OSKI_UID, "oski"); // just to be comprehensive

        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");
        uid = ref.getAuth().getUid();

        if (names.isEmpty()) {
            initializeNames();
        }
        resetFriendList(); // fill in the entire friend list

        // Add meetup invitations to the layout
        resetMeetupInvitations();

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

        recentCreation = true;
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
                    finishLayoutSetup();
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
        final String datetime = " " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(Calendar.getInstance().getTime());
        ref.child("meetups").child(uid + datetime).child("confirmed").setValue(confirmed);

        // Add the meetup to the user's ongoing meetup list
        ref.child("ongoing").child(uid).setValue(uid + datetime);

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

                    invitations.add(uid + datetime);
                    ref.child("invitations").child(ouid).setValue(invitations);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    System.out.println(firebaseError.getMessage());
                }
            });
        }

        intent.putExtra("meetup id", uid + datetime);
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

    /**
     * Currently taken care of in an alternate fashion (by resetting the entire friend list).
     * However, this may prove to be useful in the future.
     */
    private void resetSelected() {
        selected.clear();
        LinearLayout buttonContainer = (LinearLayout) findViewById(R.id.button_container);
        int numChildren = buttonContainer.getChildCount();
        for (int i = 0; i < numChildren; ++i) {
            buttonContainer.getChildAt(i).getBackground().clearColorFilter();
        }
    }

    /**
     * Adds friends to the list container in the middle of the screen.
     * For correct behavior, assumes that the container is initially empty.
     */
    private void resetFriendList() {
        updateMeetupParticipants();

        Query friendsRef = ref.child("users").child(uid);
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Object result = snapshot.child("friends").getValue();
                if (result != null) {
                    friends = (ArrayList<String>) result;
                    blockForParticipants();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[resetFriendList] Read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void finishLayoutSetup() {
        final LinearLayout ll = (LinearLayout) findViewById(R.id.button_container);
        final LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);

        boolean noFriends = true;
        for (final String fuid : friends) {
            if (meetupParticipants.contains(fuid)) {
                continue;
            }

            noFriends = false;

            final Button b = new Button(thisList);
            Query nameQRef = ref.child("uids").orderByValue().equalTo(fuid);
            nameQRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        b.setText(child.getKey());
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

        if (noFriends) {
            TextView noFriendsText = new TextView(thisList);
            String msg = "<i>You have no available friends.</i>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                noFriendsText.setText(Html.fromHtml(msg, Html.FROM_HTML_MODE_LEGACY));
            } else {
                noFriendsText.setText(Html.fromHtml(msg));
            }
            noFriendsText.setTextColor(Color.LTGRAY);
            noFriendsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

            ll.setHorizontalGravity(Gravity.CENTER);
            ll.addView(noFriendsText, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
        }
    }

    private void resetMeetupInvitations() {
        final LinearLayout ll = (LinearLayout) findViewById(R.id.meetup_container);
        final LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);

        Query inviteRef = ref.child("invitations");
        inviteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> invitations =
                        (ArrayList<String>) dataSnapshot.child(uid).getValue();

                if (invitations == null) {
                    TextView noInvitationsText = new TextView(thisList);
                    String msg = "<i>You have no meetup invitations.</i>";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        noInvitationsText.setText(Html.fromHtml(msg, Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        noInvitationsText.setText(Html.fromHtml(msg));
                    }
                    noInvitationsText.setTextColor(Color.LTGRAY);
                    noInvitationsText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

                    ll.setHorizontalGravity(Gravity.CENTER);
                    ll.addView(noInvitationsText, new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT));
                } else {
                    for (final String meetupId : invitations) {
                        int dBegin = meetupId.length() - 19, tBegin = meetupId.length() - 8;
                        String iuid = meetupId.substring(0, dBegin - 1);
                        String date = meetupId.substring(dBegin + 5, tBegin - 1);
                        String time = meetupId.substring(tBegin, meetupId.length() - 3);

                        Button b = new Button(thisList);
                        String iusername = names.get(iuid);
                        String meetupInfo = date + " meetup\n[Invited by "
                                + ((iusername == null) ? "???" : iusername) + " @ " + time + "]";
                        b.setText(meetupInfo);

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
    }
}
