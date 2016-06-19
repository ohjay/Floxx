package co.floxx.floxx;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of useful methods and values.
 * @author owenjow
 */
public class Utility {
    public static final int LAT_INF = 12345;
    public static final int LON_INF = 12345;
    public static final String BLUE_HEX = "#2196F3";
    public static final String BURGUNDY_HEX = "#800020";

    public static final HashMap<String, Integer> RES_COLORS = new HashMap<String, Integer>();
    private static boolean initialized;

    public static void initializeResColors() {
        if (!initialized) {
            RES_COLORS.put("#F44336", R.color.md_red_500);
            RES_COLORS.put("#E91E63", R.color.md_pink_500);
            RES_COLORS.put("#9C27B0", R.color.md_purple_500);
            RES_COLORS.put("#673AB7", R.color.md_deep_purple_500);
            RES_COLORS.put("#3F51B5", R.color.md_indigo_500);
            RES_COLORS.put("#2196F3", R.color.md_blue_500);
            RES_COLORS.put("#03A9F4", R.color.md_light_blue_500);
            RES_COLORS.put("#00BCD4", R.color.md_cyan_500);
            RES_COLORS.put("#009688", R.color.md_teal_500);
            RES_COLORS.put("#4CAF50", R.color.md_green_500);
            RES_COLORS.put("#8BC34A", R.color.md_light_green_500);
            RES_COLORS.put("#CDDC39", R.color.md_lime_500);
            RES_COLORS.put("#FFEB3B", R.color.md_yellow_500);
            RES_COLORS.put("#FFC107", R.color.md_amber_500);
            RES_COLORS.put("#FF9800", R.color.md_orange_500);
            RES_COLORS.put("#FF5722", R.color.md_deep_orange_500);
            RES_COLORS.put("#795548", R.color.md_brown_500);
            RES_COLORS.put("#9E9E9E", R.color.md_grey_500);
            RES_COLORS.put("#607D8B", R.color.md_blue_grey_500);
            RES_COLORS.put("#FFFFFF", R.color.md_white_500);

            initialized = true;
        }
    }

    public static void saveLatLng(final Firebase ref, final String uid, final double lat,
            final double lon) {
        Query queryRef = ref.child("locns").child(uid);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Double color = (Double) snapshot.child("color").getValue();

                Map<String, Double> map = new HashMap<String, Double>();
                map.put("latitude", lat);
                map.put("longitude", lon);
                map.put("color", color);

                ref.child("locns").child(uid).setValue(map);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[saveLatLng] Read failed: " + firebaseError.getMessage());
            }
        });
    }

    public static void saveColor(final Firebase ref, final String uid, final double color) {
        Query queryRef = ref.child("locns").child(uid);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Double lat = (Double) snapshot.child("latitude").getValue();
                Double lon = (Double) snapshot.child("longitude").getValue();

                Map<String, Double> map = new HashMap<String, Double>();
                map.put("latitude", lat);
                map.put("longitude", lon);
                map.put("color", color);

                ref.child("locns").child(uid).setValue(map);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[saveColor] Read failed: " + firebaseError.getMessage());
            }
        });
    }

    /**
     * Leaves the meetup. Enough said...
     */
    public static void leave(final String meetupId, final Firebase ref, final String uid) {
        ref.child("ongoing").child(uid).setValue(null);

        Query meetupsRef = ref.child("meetups").child(meetupId).child("confirmed");
        meetupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<String> confirmed = (ArrayList<String>) snapshot.getValue();
                if (confirmed != null) {
                    confirmed.remove(uid);
                }

                ref.child("meetups").child(meetupId).child("confirmed").setValue(confirmed);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[MeetupPortalActivity] Error: " + firebaseError.getMessage());
            }
        });
    }
}
