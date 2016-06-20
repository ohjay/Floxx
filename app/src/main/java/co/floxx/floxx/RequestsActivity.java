package co.floxx.floxx;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Official activity for the Requests page.
 * User search logic can be found in the SearchableUsers class.
 * @author owenjow
 */
public class RequestsActivity extends AppCompatActivity {
    private static final int DELAY = 500; // 500 ms
    private int numFriendsAdded;
    private Firebase ref;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        // Search view stuff
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) findViewById(R.id.user_search_bar);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        // Firebase configuration
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");
        final String currentUser = ref.getAuth().getUid().toString();

        // Populate the "received requests" area
        context = this;
        final LinearLayout layout = (LinearLayout) findViewById(R.id.received_container);

        Query queryRef = ref.child("users").orderByKey().equalTo(currentUser);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> requests;
                Object result = dataSnapshot.child(currentUser).child("requests").getValue();
                requests = (result == null) ? new ArrayList<String>() : (ArrayList<String>) result;

                for (final String senderID : requests) {
                    // Setting up the view for the name text
                    final TextView senderName = new TextView(context);
                    senderName.setTextSize(19);
                    senderName.setTypeface(Typeface.SANS_SERIF);

                    final ImageButton declineButton = new ImageButton(context);
                    final ImageButton acceptButton = new ImageButton(context);

                    // Grab the sender's actual username
                    Query nameQRef = ref.child("uids").orderByValue().equalTo(senderID);
                    nameQRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                senderName.setText(child.getKey());
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {}
                    });

                    // Setting up the accept button
                    acceptButton.setImageResource(R.drawable.ic_plusone_standard_off_client);
                    acceptButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Accept the request! Yay!
                            acceptRequest(senderID, currentUser);

                            layout.removeView(senderName);
                            layout.removeView(acceptButton);
                            layout.removeView(declineButton);
                        }
                    });

                    // Setting up the decline button
                    declineButton.setImageResource(R.drawable.ic_media_route_off_mono_dark);
                    declineButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Decline the sender's request
                            // Remove it from the registry, for starters
                            declineRequest(senderID, currentUser);

                            layout.removeView(senderName);
                            layout.removeView(acceptButton);
                            layout.removeView(declineButton);
                        }
                    });

                    // Add the name and buttons to the layout
                    layout.addView(senderName,
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    layout.addView(acceptButton,
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    layout.addView(declineButton,
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    private void acceptRequest(String senderID, String recipientID) {
        numFriendsAdded = 0; // so we know when the request has been entirely fulfilled

        addFriend(senderID, recipientID);
        addFriend(recipientID, senderID);
        delayedDestroyRequest(senderID, recipientID);

        String confirmation = FriendListActivity.names.get(senderID) + " is now your friend!";
        Toast.makeText(RequestsActivity.this, confirmation, Toast.LENGTH_LONG).show();
    }

    private void delayedDestroyRequest(final String senderID, final String recipientID) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (numFriendsAdded >= 2) {
                    // The request has been fully accepted, so we don't need it anymore
                    destroyRequest(senderID, recipientID);
                } else {
                    delayedDestroyRequest(senderID, recipientID);
                }
            }
        }, DELAY);
    }

    /**
     * Adds NEW_FRIEND to UID's friend list.
     * @param uid the ID of the person who's having a friend added
     * @param newFriend this is also a UID
     */
    private void addFriend(final String uid, final String newFriend) {
        Query queryRef = ref.child("users").orderByKey().equalTo(uid);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> friends, requests;

                Object result = dataSnapshot.child(uid).child("friends").getValue();
                friends = (result == null) ? new ArrayList<String>() : (ArrayList<String>) result;
                result = dataSnapshot.child(uid).child("requests").getValue();
                requests = (result == null) ? new ArrayList<String>() : (ArrayList<String>) result;

                if (!friends.contains(newFriend)) {
                    friends.add(newFriend);
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("friends", friends);
                    map.put("requests", requests);

                    ref.child("users").child(uid).setValue(map); // our job here is done :)
                }

                ++numFriendsAdded;
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }

    private void declineRequest(String senderID, String recipientID) {
        destroyRequest(senderID, recipientID);
        String confirmation = FriendListActivity.names.get(senderID) + "'s request has been declined.";
        Toast.makeText(RequestsActivity.this, confirmation, Toast.LENGTH_LONG).show();
    }

    private void destroyRequest(final String senderID, final String recipientID) {
        Query queryRef = ref.child("users").orderByKey().equalTo(recipientID);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> friends, requests;

                Object result = dataSnapshot.child(recipientID).child("friends").getValue();
                friends = (result == null) ? new ArrayList<String>() : (ArrayList<String>) result;
                result = dataSnapshot.child(recipientID).child("requests").getValue();
                requests = (result == null) ? new ArrayList<String>() : (ArrayList<String>) result;

                int senderIndex = requests.indexOf(senderID);
                if (senderIndex >= 0) {
                    requests.remove(senderIndex);

                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("friends", friends);
                    map.put("requests", requests);
                    ref.child("users").child(recipientID).setValue(map);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });
    }
}
