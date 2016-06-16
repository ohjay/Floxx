package co.floxx.floxx;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebase.client.Firebase;

import java.util.ArrayList;

public class MeetupPortalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetup_portal);

        Firebase.setAndroidContext(this);
        Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        final String uid = ref.getAuth().getUid().toString();

        final ArrayList<String> confirmed = new ArrayList<String>();
        Intent intent = getIntent();
        for (String ouid : intent.getExtras().keySet()) {
            if (ouid != "meetup id") {
                confirmed.add(ouid);
            }
        }

        final String meetupId = intent.getStringExtra("meetup id");

        Button b = new Button(this);
        b.setText("Meetup " + meetupId.substring(meetupId.length() - 10));

        LinearLayout ll = (LinearLayout) findViewById(R.id.meetup_container);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ll.addView(b, lp);

        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MeetupPortalActivity.this, MapActivity.class);
                for (String ouid : confirmed) {
                    if (!ouid.equals(uid)) {
                        intent.putExtra(ouid, 0);
                    }
                }

                intent.putExtra("meetup id", meetupId);
                startActivity(intent);
            }
        });
    }
}
