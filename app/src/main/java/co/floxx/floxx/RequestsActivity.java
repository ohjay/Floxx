package co.floxx.floxx;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Official activity for the Requests page, which allows people to search for friends.
 * Queries are username-based (note that usernames are necessarily distinct between users!).
 * @author owenjow
 */
public class RequestsActivity extends AppCompatActivity {
    private static final int DELAY = 500; // 500 ms
    private int numFriendsAdded;
    private Firebase ref;
    private Context context;

    static HashMap<String, String> allUsers = new HashMap<String, String>();
    private ListView listView; // where we'll put the search output
    private int progressIndex = -1, numFriends;
    private ProgressDialog dialog;
    private static final int PROGRESS_DELAY = 1000; // this is in ms!
    private HashSet<String> currentFriends;
    private String currentUsername;

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
        final String currentUser = ref.getAuth().getUid();

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
                        public void onCancelled(FirebaseError firebaseError) {
                            Log.w("RA – onCreate", "Read failed: " + firebaseError.getMessage());
                        }
                    });

                    // Setting up the accept button
                    acceptButton.setImageResource(R.drawable.ic_check_circle_white_24dp);
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
                    declineButton.setImageResource(R.drawable.ic_block_white_24dp);
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
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("RA – onCreate", "Read failed: " + firebaseError.getMessage());
            }
        });

        configureSearch();
    }

    private void configureSearch() {
        listView = (ListView) findViewById(R.id.search_list);

        final Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        final String currUser = ref.getAuth().getUid();

        // Download all usernames and UIDs (uname -> uid)
        if (allUsers.isEmpty()) {
            // Launching the progress dialog
            dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Retrieving user data...");
            dialog.show();

            Query queryRef = ref.child("users").orderByKey().equalTo(FirebaseActivity.OSKI_UID);
            queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Object result = dataSnapshot.child(FirebaseActivity.OSKI_UID)
                            .child("friends").getValue();
                    if (result != null) {
                        ArrayList<String> friends = (ArrayList<String>) result;
                        progressIndex = 0;
                        numFriends = friends.size();

                        for (final String fuid : friends) {
                            Query nameQRef = ref.child("uids").orderByValue().equalTo(fuid);
                            nameQRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                                        final String name = child.getKey();
                                        allUsers.put(name, fuid);

                                        if (fuid.equals(currUser)) {
                                            // This is the current user's username
                                            currentUsername = name;
                                        }
                                    }

                                    ++progressIndex;
                                    dialog.setProgress(progressIndex * 100 / numFriends);
                                }

                                @Override
                                public void onCancelled(FirebaseError firebaseError) {}
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    dialog.dismiss();
                }
            });
        }

        // Grab all of the user's current friends
        currentFriends = new HashSet<String>(); // clear the set to start with
        Query queryRef = ref.child("users").orderByKey().equalTo(currUser);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Object result = dataSnapshot.child(currUser).child("friends").getValue();
                if (result != null) {
                    ArrayList<String> friends = (ArrayList<String>) result;
                    numFriends += friends.size();

                    for (final String fuid : friends) {
                        Query nameQRef = ref.child("uids").orderByValue().equalTo(fuid);
                        nameQRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot child : dataSnapshot.getChildren()) {
                                    currentFriends.add(child.getKey()); // add the friend's name
                                }

                                if (dialog != null) {
                                    ++progressIndex;
                                    dialog.setProgress(progressIndex * 100 / numFriends);
                                }
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {}
                        });
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) { dialog.dismiss(); }
        });

        runProgressHandler();

        final SearchView searchView = (SearchView) findViewById(R.id.user_search_bar);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                executeSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                executeSearch(newText);
                return true;
            }
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
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("RA – addFriend", "Read failed: " + firebaseError.getMessage());
            }
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
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("RA – destroyRequest", "Read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void runProgressHandler() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressIndex >= numFriends) {
                    dialog.dismiss();
                    handleIntent(getIntent());
                } else {
                    runProgressHandler();
                }
            }
        }, PROGRESS_DELAY);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    /**
     * Gets the intent, verifies the action, and retrieves the query.
     * @param intent search intent that contains the query
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            executeSearch(query);
        }
    }

    /**
     * Searches for a user with a name matching QUERY, then displays the results.
     * @param query the search query (some username, presumably)
     */
    private void executeSearch(String query) {
        ArrayList<String> results = new ArrayList<String>();

        // TODO (Owen): exclude users whom you've already requested
        if (!query.isEmpty()) { // alternatively, maybe > 2 chars or so?
            for (String username : allUsers.keySet()) {
                if (username.equals(currentUsername) || currentFriends.contains(username)) {
                    continue; // we don't want to be able to add our curr. friends... or ourselves
                } else if (Pattern.compile(Pattern.quote(query),
                        Pattern.CASE_INSENSITIVE).matcher(username).find()) {
                    results.add(username);
                }
            }
        }

        UsersAdapter adapter = new UsersAdapter(this, results);
        listView.setAdapter(adapter);
    }

    class UsersAdapter extends BaseAdapter {
        private Context context;
        private String[] results;

        public UsersAdapter(Context context, ArrayList<String> results) {
            this.context = context;
            this.results = results.toArray(new String[results.size()]);
        }

        public int getCount() {
            return results.length;
        }

        public Object getItem(int position) {
            return results[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            EntryView entryView;

            if (convertView == null) {
                entryView = new EntryView(context, results[position]);
            } else {
                entryView = (EntryView) convertView;
                String username = results[position];
                entryView.setTextContent(username);
            }

            return entryView;
        }
    }

    void sendFriendRequest(final String username) {
        Firebase.setAndroidContext(this);
        final Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        final String senderID = ref.getAuth().getUid(); // current user
        final String recipientID = allUsers.get(username);

        // Save sender UID under /users/<recipient UID>/requests
        // For the future: it'd be nice to store the date as well
        Query queryRef = ref.child("users").orderByKey().equalTo(recipientID);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> friends, requests;

                // First, we'll grab the user's current friend/request lists
                // Note (- Owen 6/16): we grab the friend list so that it doesn't get erased
                Object result = dataSnapshot.child(recipientID).child("friends").getValue();
                friends = (result == null) ? new ArrayList<String>() : (ArrayList<String>) result;
                result = dataSnapshot.child(recipientID).child("requests").getValue();
                requests = (result == null) ? new ArrayList<String>() : (ArrayList<String>) result;

                if (!requests.contains(senderID)) { // no duplicates, man
                    requests.add(senderID);
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("friends", friends);
                    map.put("requests", requests);
                    ref.child("users").child(recipientID).setValue(map);

                    String confirmation = "Your invitation has been sent!";
                    Toast.makeText(RequestsActivity.this, confirmation, Toast.LENGTH_LONG).show();
                } else {
                    String confirmation = "You've already sent " + username + " an invitation!";
                    Toast.makeText(RequestsActivity.this, confirmation, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("RA – sendFReq", "Read failed: " + firebaseError.getMessage());
            }
        });
    }

    class EntryView extends LinearLayout {
        private TextView nameView;
        private ImageButton requestButton;

        public EntryView(Context context, final String username) {
            super(context);
            this.setOrientation(VERTICAL);

            // Creating the nameView (which should contain a username)
            nameView = new TextView(context);
            nameView.setText(username);
            nameView.setTextSize(19);
            nameView.setTextColor(Color.BLACK);
            nameView.setTypeface(Typeface.SANS_SERIF);

            // Creating the image button (the ADD symbol)
            requestButton = new ImageButton(context);
            requestButton.setImageResource(R.drawable.ic_add_circle_white_24dp);

            requestButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Send a connection request
                    sendFriendRequest(username);
                    removeView(nameView);
                    removeView(requestButton);
                }
            });

            addView(nameView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            addView(requestButton, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT)); // can set .gravity = Gravity.RIGHT
        }

        public void setTextContent(String content) {
            nameView.setText(content);
        }
    }
}
