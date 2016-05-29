package co.floxx.floxx;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Searches the user list for potential friends.
 * Queries are username-based (note that usernames are necessarily distinct between users!).
 * @author owenjow
 */
public class SearchableUsers extends Activity {
    static HashMap<String, String> allUsers = new HashMap<String, String>();
    private ListView listView; // where we'll put the search output

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        listView = (ListView) findViewById(R.id.search_list);

        // Download all usernames and UIDs (uname -> uid)
        if (allUsers.isEmpty()) {
            final Firebase ref = new Firebase("https://floxx.firebaseio.com/");
            Query queryRef = ref.child("users").orderByKey().equalTo(FirebaseActivity.OSKI_UID);
            queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Object result = dataSnapshot.child(FirebaseActivity.OSKI_UID)
                            .child("friends").getValue();
                    if (result != null) {
                        ArrayList<String> friends = (ArrayList<String>) result;

                        for (final String fuid : friends) {
                            Query nameQRef = ref.child("uids").orderByValue().equalTo(fuid);
                            nameQRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                                        final String name = child.getKey();
                                        allUsers.put(name, fuid);
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
        }

        handleIntent(getIntent());
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

        for (String username : allUsers.keySet()) {
            if (Pattern.compile(Pattern.quote(query),
                    Pattern.CASE_INSENSITIVE).matcher(username).find()) {
                results.add(username);
            }
        }

        System.out.println("===== RESULTS ======");
        for (String result : allUsers.keySet()) {
            System.out.println("- " + result);
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

    class EntryView extends LinearLayout {
        private TextView nameView;

        public EntryView(Context context, String username) {
            super(context);
            this.setOrientation(VERTICAL);

            nameView = new TextView(context);
            nameView.setText(username);
            nameView.setTextSize(19);
            nameView.setTextColor(Color.BLACK);
            nameView.setTypeface(Typeface.SANS_SERIF);

            addView(nameView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
        }

        public void setTextContent(String content) {
            nameView.setText(content);
        }
    }
}
