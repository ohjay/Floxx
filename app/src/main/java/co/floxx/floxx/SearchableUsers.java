package co.floxx.floxx;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Searches the user list for potential friends.
 * Queries are username-based (note that usernames are necessarily distinct between users!).
 * @author owenjow
 */
public class SearchableUsers extends Activity {
    static HashMap<String, String> allUsers = new HashMap<String, String>();
    private ListView listView; // where we'll put the search output
    private int progressIndex = -1, numFriends;
    private boolean isDownloading;
    private ProgressDialog dialog;
    private static final int PROGRESS_DELAY = 1000; // this is in ms!

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        listView = (ListView) findViewById(R.id.search_list);

        // Download all usernames and UIDs (uname -> uid)
        if (allUsers.isEmpty()) {
            isDownloading = true;
            dialog = new ProgressDialog(this);
            dialog.setMessage("Retrieving user data...");
            dialog.show();

            final Firebase ref = new Firebase("https://floxx.firebaseio.com/");
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

        if (isDownloading) {
            runProgressHandler();
        } else {
            handleIntent(getIntent());
        }

        final SearchView searchView = (SearchView) findViewById(R.id.user_search_bar);
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
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

    private void runProgressHandler() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressIndex == numFriends) {
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

        // TODO (Owen): don't include current friends or the user himself
        if (!query.isEmpty()) { // alternatively, maybe > 2 chars or so?
            for (String username : allUsers.keySet()) {
                if (Pattern.compile(Pattern.quote(query),
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
        final String senderID = ref.getAuth().getUid().toString(); // current user
        final String recipientID = allUsers.get(username);

        // Save sender UID under /users/<recipient UID>/requests
        // For the future: it'd be nice to store the date as well
        Query queryRef = ref.child("users").orderByKey().equalTo(recipientID);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> friends, requests;

                // First, we'll grab the user's current friend/request lists
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
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
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
            requestButton.setImageResource(R.drawable.ic_plusone_standard_off_client);

            requestButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Send a connection request
                    sendFriendRequest(username);
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