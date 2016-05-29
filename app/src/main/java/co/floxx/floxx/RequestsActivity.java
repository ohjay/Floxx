package co.floxx.floxx;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class RequestsActivity extends AppCompatActivity {
    static HashMap<String, String> allUsers = new HashMap<String, String>();
    RequestsActivity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        context = this;

        // Download all usernames and UIDs (uid -> uname)
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
                                        allUsers.put(fuid, name);
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
    }
}
