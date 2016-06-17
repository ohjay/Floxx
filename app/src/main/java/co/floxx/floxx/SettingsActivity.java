package co.floxx.floxx;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.thebluealliance.spectrum.SpectrumDialog;
import com.thebluealliance.spectrum.SpectrumDialog.OnColorSelectedListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    final Context context = this;
    private Firebase ref;
    private String currentUser;
    private int selectedColorRes = R.color.md_blue_500;

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
                AlertDialog alertDialog = new AlertDialog.Builder(SettingsActivity.this).create();
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
                Intent intent = new Intent(SettingsActivity.this, RequestsActivity.class);
                startActivity(intent);
            }
        });

        Button personalizationButton = (Button) findViewById(R.id.personalize_marker);
        personalizationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Open a dialog and let users choose colors for their markers
                makeColorDialog();
            }
        });

        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");
        currentUser = ref.getAuth().getUid().toString();

        // Set SELECTED_COLOR_RES if applicable
        Query queryRef = ref.child("locns").child(currentUser);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Double color = (Double) snapshot.child("color").getValue();
                if (color != null) {
                    Utility.initializeResColors();
                    String hexStr = "#" + Integer.toHexString(color.intValue()).toUpperCase();

                    if (Utility.RES_COLORS.containsKey(hexStr)) {
                        selectedColorRes = Utility.RES_COLORS.get(hexStr);
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError error) {
                System.out.println("[SettingsActivity] Read failed: " + error.getMessage());
            }
        });

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

    private void makeColorDialog() {
        OnColorSelectedListener ocsl = new OnColorSelectedListener() {
            @Override
            public void onColorSelected(boolean positiveResult, @ColorInt int color) {
                if (positiveResult) {
                    // Save the user's marker color to Firebase
                    String hexStr = Integer.toHexString(color);
                    hexStr = "#" + hexStr.substring(hexStr.length() - 6);
                    Utility.saveColor(ref, currentUser, Integer.parseInt(hexStr.substring(1), 16));

                    if (Utility.RES_COLORS.containsKey(hexStr.toUpperCase())) {
                        selectedColorRes = Utility.RES_COLORS.get(hexStr.toUpperCase());
                    }
                }
            }
        };

        FragmentManager fm = getSupportFragmentManager();
        new SpectrumDialog.Builder(context).setColors(R.array.md_colors)
                .setSelectedColorRes(selectedColorRes).setDismissOnColorSelected(false)
                .setOutlineWidth(2).setOnColorSelectedListener(ocsl)
                .build().show(fm, "color_dialog");
    }
}
