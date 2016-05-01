package co.floxx.floxx;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Map;

public class fireBaseActivity extends AppCompatActivity {
    Firebase myFirebaseRef = new Firebase("https://floxx.firebaseio.com/");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fire_base);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        String euser = extras.getString("enter_username");
        String epass = extras.getString("enter_password");
        String ruser = extras.getString("register_user");
        String rpass = extras.getString("register_password");


//        if (euser == null) {
//            myFirebaseRef.child(ruser).setValue(rpass);
//            myFirebaseRef.child(ruser).addValueEventListener(new ValueEventListener() {
//
//                @Override
//                public void onDataChange(DataSnapshot snapshot) {
//                    System.out.println(snapshot.getValue());  //prints "Do you have data? You'll love Firebase."
//                }
//
//                @Override
//                public void onCancelled(FirebaseError error) {
//                }
//
//            });
//            intent = new Intent(fireBaseActivity.this, FullscreenActivity.class);
//            Log.i("message","register complete!");
//            fireBaseActivity.this.startActivity(intent);
//        }
//        else {
//            myFirebaseRef.child(euser).addValueEventListener(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot snapshot) {
//                    System.out.println(snapshot.getValue());  //prints "Do you have data? You'll love Firebase."
//                }
//                @Override public void onCancelled(FirebaseError error) { }
//            });
//        }
        if (euser == null) {
            String email = ruser + "-" + "@gmail.com";
            Log.i(email, "should be registering this");
            myFirebaseRef.createUser(email, rpass, new Firebase.ValueResultHandler<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    System.out.println("Successfully created user account with uid: " + result.get("uid"));
                }

                @Override
                public void onError(FirebaseError firebaseError) {
                    // there was an error
                }
            });
            intent = new Intent(fireBaseActivity.this, FullscreenActivity.class);
            Log.i("message","Register Successful!");
            fireBaseActivity.this.startActivity(intent);
        }
        else {
            String email1 = euser + "-" + "@gmail.com";
            myFirebaseRef.authWithPassword(email1, epass, new Firebase.AuthResultHandler() {
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
                    Log.i("message","Login failed!");
                    fireBaseActivity.this.startActivity(intent);
                }
            });
        }
    }
}
