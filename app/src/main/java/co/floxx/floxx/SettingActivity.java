package co.floxx.floxx;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.firebase.client.Firebase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SettingActivity extends AppCompatActivity {
    final Context context = this;
    private Firebase ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ImageButton button = (ImageButton) findViewById(R.id.left_arrow);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button more = (Button) findViewById(R.id.TeaEra);
        more.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(SettingActivity.this).create();
                alertDialog.setTitle("Is Tea Era open today?");

                SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
                Date d = new Date();
                String dayOfTheWeek = sdf.format(d);
                Log.i("day", dayOfTheWeek);
                if (dayOfTheWeek.equals("Tuesday"))
                    alertDialog.setMessage("Nope :(");
                else
                    alertDialog.setMessage("Yup!");
                alertDialog.setButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // here you can add functions
                    }
                });

                alertDialog.show(); // see this!
            }

        });

        Button addFriends = (Button) findViewById(R.id.add_friends);
        addFriends.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this, RequestsActivity.class);
                startActivity(intent);
            }
        });

        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");
        final String currentUser = ref.getAuth().getUid().toString();

        Button permissionsButton = (Button) findViewById(R.id.locn_permissions_button);
        permissionsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(context);
                View dialogView = li.inflate(R.layout.permission_dialog, null);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                dialogBuilder.setView(dialogView);

                dialogBuilder
                        .setPositiveButton("On", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Turn on location services if they're not on already
                                setLocationPermissions(currentUser, "on");
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Off", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Turn off location services if they're not off already
                                setLocationPermissions(currentUser, "off");
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
            }
        });
    }

    private void setLocationPermissions(String uid, String permissionValue) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("location", permissionValue);
        ref.child("permissions").child(uid).setValue(map);
    }
}
