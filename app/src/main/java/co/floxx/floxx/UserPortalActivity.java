package co.floxx.floxx;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.thebluealliance.spectrum.SpectrumDialog;
import com.thebluealliance.spectrum.SpectrumDialog.OnColorSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserPortalActivity extends AppCompatActivity {
    final Context context = this;
    private Firebase ref;
    private String currentUser;
    private int selectedColorRes = R.color.md_blue_500;
    private String currPermissions = "on";

    @Override
    protected void onResume() {
        super.onResume();
        grabLocnPermissions(currentUser);
        final Button finalButton = (Button) findViewById(R.id.final_button); // it's final get it??

        // Reset the functionality of the final button
        Query ongoingRef = ref.child("ongoing");
        ongoingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(currentUser)) {
                    final String meetupId = snapshot.child(currentUser).getValue().toString();

                    Query meetupsRef = ref.child("meetups").child(meetupId).child("confirmed");
                    meetupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot snapshot) {
                            finalButton.setText("Meetup portal");
                            finalButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(UserPortalActivity.this,
                                            MeetupPortalActivity.class);

                                    ArrayList<String> confirmed =
                                            (ArrayList<String>) snapshot.getValue();
                                    for (String ouid : confirmed) {
                                        if (!ouid.equals(currentUser)) {
                                            intent.putExtra(ouid, 0);
                                        }
                                    }
                                    intent.putExtra("meetup id", meetupId);
                                    startActivity(intent);
                                }
                            });
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            Log.w("UPA – onResume", "Read failed: " + firebaseError.getMessage());
                        }
                    });
                } else {
                    // We want the group creation (fl) portal
                    finalButton.setText("Group creation portal");
                    finalButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(UserPortalActivity.this,
                                    FriendListActivity.class);
                            UserPortalActivity.this.startActivity(intent);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("UPA – onResume", "Read failed: " + firebaseError.getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Log this fool out
        super.onBackPressed();
        new Firebase("https://floxx.firebaseio.com/").unauth();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase initialization
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");
        currentUser = ref.getAuth().getUid();

        Intent intent = getIntent();
        String subtext = "Logged in as " + "<b><font color='#FFFFFF'>"
                + intent.getStringExtra("username") + "</font></b>";
        setContentView(R.layout.activity_user_portal);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            ((TextView) findViewById(R.id.up_subtext)).setText(
                    Html.fromHtml(subtext, Html.FROM_HTML_MODE_LEGACY));
        } else {
            ((TextView) findViewById(R.id.up_subtext)).setText(Html.fromHtml(subtext));
        }

        Typeface montserrat = Typeface.createFromAsset(getAssets(), "Montserrat-Regular.otf");
        ((TextView) findViewById(R.id.up_header)).setTypeface(montserrat);

        initializeSignOutButton();
        initializeTeaEraButton();
        initializePermissionsButton();
        initializeFinalButton();

        ImageButton addFriends = (ImageButton) findViewById(R.id.manage_friends);
        addFriends.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(UserPortalActivity.this, RequestsActivity.class);
                startActivity(intent);
            }
        });

        ImageButton personalizationButton = (ImageButton) findViewById(R.id.customize_marker);
        personalizationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Open a dialog and let users choose colors for their markers
                makeColorDialog();
            }
        });

        setSelectedColorResIfApplicable();
    }

    private void initializeSignOutButton() {
        Button signOutButton = (Button) findViewById(R.id.sign_out);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For the lingerers
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction("co.floxx.floxx.ACTION_LOGOUT");
                sendBroadcast(broadcastIntent);
                ref.unauth();
                finish();
            }
        });
    }

    private void initializeTeaEraButton() {
        ImageButton teButton = (ImageButton) findViewById(R.id.tea_era);
        teButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(UserPortalActivity.this).create();
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
                        // We should probably check if the user is a goat here
                    }
                });

                alertDialog.show(); // see this!
            }

        });
    }

    private void initializePermissionsButton() {
        ImageButton permissionsButton = (ImageButton) findViewById(R.id.toggle_locn_services);
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

                TextView currStateText = new TextView(UserPortalActivity.this);
                String currState = "(Location services are currently <b>" + currPermissions + "</b>.)";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    currStateText.setText(Html.fromHtml(currState, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    currStateText.setText(Html.fromHtml(currState));
                }
                LinearLayout ll = (LinearLayout) dialogView.findViewById(R.id.permission_root);
                ll.addView(currStateText);

                alertDialog.show();
            }
        });
    }

    private void initializeFinalButton() {
        Button finalButton = (Button) findViewById(R.id.final_button);
        if (Intermediary.firebaseMeetupId != null) {
            finalButton.setText("Meetup portal");
        } else {
            finalButton.setText("Group creation portal");
        }

        finalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Intermediary.firebaseMeetupId == null) {
                    Intent intent = new Intent(UserPortalActivity.this,
                            FriendListActivity.class);
                    UserPortalActivity.this.startActivity(intent);
                    return;
                }

                Intent intent = new Intent(UserPortalActivity.this,
                        MeetupPortalActivity.class);
                for (String ouid : Intermediary.firebaseConfirmed) {
                    if (!ouid.equals(currentUser)) {
                        intent.putExtra(ouid, 0);
                    }
                }
                intent.putExtra("meetup id", Intermediary.firebaseMeetupId);

                Intermediary.firebaseMeetupId = null;
                startActivity(intent);
            }
        });
    }

    private void setSelectedColorResIfApplicable() {
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
                System.out.println("[UserPortalActivity] Read failed: " + error.getMessage());
            }
        });
    }

    private void setLocationPermissions(String uid, String permissionValue) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("location", permissionValue);
        ref.child("permissions").child(uid).setValue(map);
        currPermissions = permissionValue;
    }

    private void grabLocnPermissions(final String uid) {
        Query queryRef = ref.child("permissions").child(uid).child("location");
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currPermissions = dataSnapshot.getValue().toString();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("grabLocnPermissions", "Read failed: " + firebaseError.getMessage());
            }
        });
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
