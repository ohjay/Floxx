package co.floxx.floxx;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class fireBaseActivity extends AppCompatActivity {
    String euser, epass, rpass, ruser, uid;
    Firebase ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");

        setContentView(R.layout.activity_fire_base);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        euser = extras.getString("enter_username");
        epass = extras.getString("enter_password");
        ruser = extras.getString("register_user");
        rpass = extras.getString("register_password");

        if (euser == null) {
            String email = ruser + "@gmail.com";
            Log.i(email, "should be registering this");
            ref.createUser(email, rpass, new Firebase.ValueResultHandler<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    uid = result.get("uid").toString();
                    System.out.println("Successfully created user account with uid: " + uid);

                    // Add the username and UID to Firebase
                    Map<String, Object> uidMap = new HashMap<String, Object>();
                    uidMap.put(ruser, uid);
                    ref.child("uids").updateChildren(uidMap);

                    // Check for Oski, friend to everyone
                    Query queryRef = ref.child("uids").orderByValue().equalTo("f58da7f2-897a-4b4f-85a2-03a66ca33ad2");
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
                }

                @Override
                public void onError(FirebaseError firebaseError) {
                    // there was an error
                }
            });
            intent = new Intent(fireBaseActivity.this, FullscreenActivity.class);
            Log.i("message","Register Successful!");
            fireBaseActivity.this.startActivity(intent);
        } else {
            String email1 = euser + "@gmail.com";
            ref.authWithPassword(email1, epass, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    System.out.println("User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());
                    Intent intent = new Intent(fireBaseActivity.this, ActivityFriendList.class);
                    Log.i("message","Login Success!");
                    fireBaseActivity.this.startActivity(intent);
                }
                @Override
                public void onAuthenticationError(FirebaseError firebaseError) {
                    Intent intent = new Intent(fireBaseActivity.this, FullscreenActivity.class);
                    Log.i("message", "Login failed!");
                    fireBaseActivity.this.startActivity(intent);
                }
            });
        }
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
     */
    protected void addToFriends(final String newUID, final String uid, final Firebase ref) {
        // Get the user's current friend list
        Query queryRef = ref.child("users").orderByKey().equalTo(uid);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> friends;

                Object result = dataSnapshot.child("f58da7f2-897a-4b4f-85a2-03a66ca33ad2").child("friends").getValue();
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
                // We can do something here if we want
            }
        });
    }
}
