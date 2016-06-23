package co.floxx.floxx;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.github.silvestrpredko.dotprogressbar.DotProgressBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FirebaseActivity extends AppCompatActivity {
    public static final String OSKI_UID = "07750a2b-0f39-494d-ab19-59a6d3a276cc";
    String euser, epass, rpass, ruser, remail, uid; // "e" for sign in. "r" for registration
    private String nodots; // REMAIL without dots (so it can be saved as a Firebase key)
    private boolean emailChecked;
    Firebase ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");

        setContentView(R.layout.activity_firebase);

        Typeface montserrat = Typeface.createFromAsset(getAssets(), "Montserrat-Regular.otf");
        ((TextView) findViewById(R.id.connecting)).setTypeface(montserrat);

        DotProgressBar progressBar = (DotProgressBar) findViewById(R.id.firebase_progress);
        progressBar.setStartColor(ContextCompat.getColor(this, R.color.berkeley_blue));
        progressBar.setEndColor(ContextCompat.getColor(this, R.color.owen_gold));
        progressBar.setDotAmount(6);
        progressBar.setAnimationTime(300);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        euser = extras.getString("enter_username");
        epass = extras.getString("enter_password");
        ruser = extras.getString("register_user");
        remail = extras.getString("register_email");
        rpass = extras.getString("register_password");

        if (euser == null) {
            if (!isEmailValid(remail)) {
                Toast toast = Toast.makeText(this, "That's not a valid email!",
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 10);
                toast.show();
                finish();
                return; // not sure if this is necessary, but it won't hurt
            }

            // User registration
            // To start: check if REMAIL is unique. Used as a deterrent for account creation spam

            emailChecked = false;
            nodots = remail.substring(0, remail.lastIndexOf("."));
            ref.child("emails").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (emailChecked) { return; }

                    for (DataSnapshot child : snapshot.getChildren()) {
                        final String storedEmail = child.getKey();
                        if (storedEmail.equals(nodots)) {
                            // Exit; we can't have duplicates
                            Toast toast = Toast.makeText(FirebaseActivity.this,
                                    "Email already in use. :(", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 10);
                            toast.show();
                            finish();
                            return;
                        }
                    }

                    emailChecked = true;
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.e("Read failed: ", firebaseError.getMessage());
                }
            });

            blockUntilEmailChecked();
        } else {
            // Login with an existing account
            String email1 = euser + "@gmail.com";
            ref.authWithPassword(email1, epass, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    uid = authData.getUid();
                    System.out.println("User ID: " + uid + ", Provider: " + authData.getProvider());
                    redirectAuthenticatedUser();
                }
                @Override
                public void onAuthenticationError(FirebaseError firebaseError) {
                    Intent intent = new Intent(FirebaseActivity.this, FullscreenActivity.class);
                    Toast toast = Toast.makeText(FirebaseActivity.this,
                            firebaseError.getMessage(), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 30);
                    toast.show();
                    FirebaseActivity.this.startActivity(intent);
                    finish();
                }
            });
        }
    }

    private void finishRegistration() {
        // TODO: Send confirmation email to REMAIL
        // User should not be fully registered until he/she confirms

        String email = ruser + "@gmail.com"; // this doesn't need to change (- Owen 5/30)
        Log.i(email, "should be registering this");
        ref.createUser(email, rpass, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                uid = result.get("uid").toString();
                System.out.println("Successfully created user account with uid: " + uid);
                Toast toast = Toast.makeText(FirebaseActivity.this, "You were able to register!",
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 10);
                toast.show();

                // Add the username and UID to Firebase
                Map<String, Object> uidMap = new HashMap<String, Object>();
                uidMap.put(ruser, uid);
                ref.child("uids").updateChildren(uidMap);

                // Check for Oski, friend to everyone
                Query queryRef = ref.child("uids").orderByValue().equalTo(OSKI_UID);
                queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Object oskId = dataSnapshot.child("oski").getValue();
                        if (oskId == null) {
                            // He doesn't exist, so we'll create him
                            createOski(uid);
                        } else {
                            String oskiUID = oskId.toString();
                            // Add the new user to Oski's friend base
                            addToFriends(uid, oskiUID, ref);
                            // Add Oski to the new user's friend base
                            addToFriends(oskiUID, uid, ref);
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        // We can do something here if we want
                    }
                });

                // Save REMAIL to Firebase along with associated UID
                Map<String, Object> emailMap = new HashMap<String, Object>();
                emailMap.put(nodots, uid);
                ref.child("emails").updateChildren(emailMap);

                // Default the user's location permissions to "on" (sorry Tony)
                ref.child("permissions").child(uid).child("location").setValue("on");
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                String message = firebaseError.getMessage();
                if (message.startsWith("The specified email address is already in use")) {
                    message = "The specified username is already in use.";
                }
                Toast toast = Toast.makeText(FirebaseActivity.this, message, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            }
        });
        Intent intent = new Intent(FirebaseActivity.this, FullscreenActivity.class);
        FirebaseActivity.this.startActivity(intent);
        finish();
    }

    private void blockUntilEmailChecked() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (emailChecked) {
                    finishRegistration();
                } else {
                    blockUntilEmailChecked();
                }
            }
        }, 500);
    }

    /**
     * Starts either the map activity or the friend list activity,
     * depending on whether the user is already in a meetup.
     */
    private void redirectAuthenticatedUser() {
        Query ongoingRef = ref.child("ongoing");
        ongoingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(uid)) {
                    signalMeetupPortal(snapshot.child(uid).getValue().toString());
                } else {
                    // Signal the friend list (/group creation) page
                    Intent intent = new Intent(FirebaseActivity.this, UserPortalActivity.class);
                    intent.putExtra("username", euser);
                    FirebaseActivity.this.startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    private void signalMeetupPortal(final String meetupId) {
        // Get the users who are currently in the meetup
        Query meetupsRef = ref.child("meetups").child(meetupId).child("confirmed");
        meetupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Intermediary.firebaseConfirmed = new ArrayList<String>(
                        (ArrayList<String>) snapshot.getValue());
                Intermediary.firebaseMeetupId = meetupId;

                Intent intent = new Intent(FirebaseActivity.this, UserPortalActivity.class);
                intent.putExtra("username", euser);
                FirebaseActivity.this.startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("FA – signalMeetupPortal", "Read failed: " + firebaseError.getMessage());
            }
        });
    }

    /**
     * Creates a user named Oski and updates the global oskiUID variable with his UID.
     */
    protected void createOski(final String uid) {
        ref.createUser("oski@gmail.com", "oski", new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                // Add the username and UID to Firebase
                Map<String, Object> uidMap = new HashMap<String, Object>();
                String oskId = result.get("uid").toString();
                uidMap.put("oski", oskId);
                ref.child("uids").updateChildren(uidMap);

                // Add the new user to Oski's friend base
                addToFriends(uid, oskId, ref);
                // Add Oski to the new user's friend base
                addToFriends(oskId, uid, ref);
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                // There was an error
            }
        });
    }

    /**
     * Adds the user with NEW_UID to user UID's friend list.
     * IMPORTANT: ONLY USE THIS METHOD FOR NEW USERS, OR IT MAY ERASE FRIEND REQUESTS (- Owen 5/29).
     */
    protected void addToFriends(final String newUID, final String uid, final Firebase ref) {
        // Get the user's current friend list
        Query queryRef = ref.child("users").orderByKey().equalTo(uid);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> friends;

                // Owen 5/29: is this correct? Should OSKI_UID maybe be uid?
                Object result = dataSnapshot.child(OSKI_UID).child("friends").getValue();
                if (result != null) {
                    friends = (ArrayList<String>) result;
                } else {
                    // This guy has no friends :(
                    friends = new ArrayList<String>();
                }

                friends.add(newUID);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("friends", friends);
                ref.child("users").child(uid).setValue(map);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("FA – addToFriends", "Read failed: " + firebaseError.getMessage());
            }
        });
    }

    /**
     * Checks whether or not an email is valid.
     * @param target an email
     * @return true if the email is of a legitimate form
     */
    public static boolean isEmailValid(CharSequence target) {
        if (target == null) {
            return false;
        }

        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
