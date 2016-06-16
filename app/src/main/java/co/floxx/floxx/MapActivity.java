package co.floxx.floxx;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerDragListener {
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int FINE_REQ_CODE = 13;
    private final static int COARSE_REQ_CODE = 14;
    private boolean initialZoom = true;
    private LocationRequest mLocationRequest;
    private GoogleMap mMap;
    private Location mLastLocation;
    private Marker marker, meetingMarker;
    private static HashMap<String, Marker> others = new HashMap<String, Marker>(); // ouid -> oMarker
    private String meetupId = "";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    GoogleApiClient mGoogleApiClient;
    public static final String TAG = MapActivity.class.getSimpleName();

    //================================================================================
    // Listener methods
    //================================================================================
    // - onStart
    // - onStop
    // - onCreate (gets all the initialization stuff started)
    // - onMapReady (saves the map and launches the setup code)
    // - onMapClick (places meeting markers)
    // - onResume
    // - onPause
    // - onPostCreate
    // - onConnected
    // - onRequestPermissionsResult
    // - onConnectionSuspended
    // - onConnectionFailed
    // - onLocationChanged
    // - onMarkerDrag
    // - onMarkerDragEnd
    // - onMarkerDragStart
    //================================================================================

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        // Unpack intent extras (user collection + meetup ID)
        for (String ouid : intent.getExtras().keySet()) {
            if (ouid != "meetup id") {
                others.put(ouid, null);
            }
        }
        meetupId = intent.getStringExtra("meetup id");

        setContentView(R.layout.activity_map);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        setUpMapIfNeeded();

        final Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        for (final String ouid : others.keySet()) {
            setOtherMarkerFull(ouid, ref);
        }

        // Setting a data listener on the meeting point
        Firebase locnRef = new Firebase("https://floxx.firebaseio.com/meetups/" + meetupId + "/location");
        locnRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double latitude = (double) snapshot.child("latitude").getValue();
                    double longitude = (double) snapshot.child("longitude").getValue();

                    updateMeetingMarker(latitude, longitude);
                }
            }

            @Override
            public void onCancelled(FirebaseError e) {
                System.out.println("Read failed: " + e.getMessage());
            }
        });

        final String uid = ref.getAuth().getUid().toString();

        // Setting a data listener on the confirmed meetup users
        Firebase confRef = new Firebase("https://floxx.firebaseio.com/meetups/" + meetupId + "/confirmed");
        confRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) { return; }

                ArrayList<String> confirmed = (ArrayList<String>) snapshot.getValue();
                Set<String> veterans = others.keySet();

                for (String ouid : confirmed) {
                    if (!ouid.equals(uid) && !veterans.contains(ouid)) {
                        others.put(ouid, null);
                        setOtherMarkerFull(ouid, ref);
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError e) {
                System.out.println("Read failed: " + e.getMessage());
            }
        });

        // TODO (OJ?): If this works, we should probably migrate all locn updates to this service
        LocationUpdateService.startUpdates(this, uid, mGoogleApiClient, mLocationRequest);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (mMap != null) {
            setUpMap();
            mMap.setOnMapClickListener(this);
            mMap.setOnMarkerDragListener(this);
        }
    }

    @Override
    public void onMapClick(LatLng point) {
        if (meetingMarker != null) { return; } // only ONE of these on the map!!
        MarkerOptions mmo = new MarkerOptions().position(point).title("meet here!").draggable(true);
        meetingMarker = mMap.addMarker(mmo); // create the almighty meeting marker

        computeETAs();

        mMap.setOnMapClickListener(null); // we only want to be adding one marker
        saveMeetupLocation(point.latitude, point.longitude);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Location services connected.");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_REQ_CODE);

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    COARSE_REQ_CODE);
            return;
        }

        startLocationUpdates();

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                    mLocationRequest, this);
        } else {
            handleNewLocation(mLastLocation);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        switch (requestCode) {
            case FINE_REQ_CODE: {
                mMap.setMyLocationEnabled(true);
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation == null) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                            mLocationRequest, this);
                } else {
                    handleNewLocation(mLastLocation);
                }
                return;
            }

            case COARSE_REQ_CODE: {
                mMap.setMyLocationEnabled(true);
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation == null) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                            mLocationRequest, this);
                } else {
                    handleNewLocation(mLastLocation);
                }
                return;
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code "
                    + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        // Do something? Live ETA update when dragging?
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng markerPos = marker.getPosition();
        saveMeetupLocation(markerPos.latitude, markerPos.longitude);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        // Do something?
    }

    //================================================================================
    // Setup methods
    //================================================================================
    // - setUpMap
    // - setUpMapIfNeeded
    // - startLocationUpdates
    // - attachBaseContext
    //================================================================================

    private void setUpMap() {
        if (mLastLocation == null) {
            if (marker != null) { marker.remove(); }
            marker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            marker.showInfoWindow();
        } else {
            LocationManager locationManager =
                    (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            try {
                Location location =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (marker != null) { marker.remove(); }
                marker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),
                        location.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                marker.showInfoWindow();
            } catch (SecurityException e) {
                e.printStackTrace(); // lol
            }
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment
            MapFragment mapFragment = (MapFragment) getFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            // Check if we were successful in obtaining the map
            if (mMap != null) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                } else {
                    // Show rationale and request permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            FINE_REQ_CODE);

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            COARSE_REQ_CODE);
                }

                setUpMap();
            }
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    //================================================================================
    // Utility/update classes and methods
    //================================================================================
    // - handleNewLocation (what to do when we have a location update for the user)
    // - saveMeetupLocation (saves location to Firebase so other users can access it)
    // - updateMeetingMarker (updates the meeting marker position)
    // - computeETAs (computes ETA and distance information for other users)
    // - setOtherMarkerFull (creates a listener whose callback = setOtherMarker)
    // - setOtherMarker (places a marker for a user that isn't on the map already)
    // - Pair (POJO that serves as a ouid/queryRes data bundle)
    // - DirQueryTask (asynchronous task that queries the Directions API)
    // - toggle
    // - hide
    // - show
    // - delayedHide
    //================================================================================

    public void handleNewLocation(Location location) {
        Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        Map<String, Double> map = new HashMap<String, Double>();
        map.put("latitude", location.getLatitude());
        map.put("longitude", location.getLongitude());
        ref.child("locns").child(ref.getAuth().getUid().toString()).setValue(map);

        Log.d(TAG, location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        if (marker != null) { marker.remove(); }
        marker = mMap.addMarker(options);
        marker.showInfoWindow();

        if (initialZoom) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
            initialZoom = false;
        }
    }

    public void saveMeetupLocation(double latitude, double longitude) {
        Map<String, Double> map = new HashMap<String, Double>();
        map.put("latitude", latitude);
        map.put("longitude", longitude);

        Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        ref.child("meetups").child(meetupId).child("location").setValue(map);
    }

    private void updateMeetingMarker(double latitude, double longitude) {
        if (meetingMarker == null) {
            MarkerOptions mmo = new MarkerOptions().position(new LatLng(latitude, longitude))
                    .title("meet here!").draggable(true);
            meetingMarker = mMap.addMarker(mmo); // create the meeting marker
        } else {
            meetingMarker.setPosition(new LatLng(latitude, longitude));
        }

        computeETAs();
    }

    private void computeETAs() {
        if (meetingMarker == null) { return; }

        LatLng meetingPos = meetingMarker.getPosition();
        Iterator it = others.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String ouid = (String) pair.getKey();
            Marker oMarker = (Marker) pair.getValue();

            if (oMarker != null) {
                LatLng oMarkerPos = oMarker.getPosition();

                // TODO (Owen): add API key and request "Maps-official" travel time
                new DirQueryTask().execute("http://maps.googleapis.com/maps/api/directions/json?origin="
                        + oMarkerPos.latitude + "," + oMarkerPos.longitude + "&destination="
                        + meetingPos.latitude + "," + meetingPos.longitude + "&mode=driving&sensor=false",
                        ouid);
            }
        }
    }

    private void setOtherMarkerFull(final String ouid, Firebase ref) {
        Query llqRef = ref.child("locns").child(ouid).orderByKey();
        llqRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                setOtherMarker(ouid, dataSnapshot);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {}
        });

        // Set a listener on the other guy, so we can see when his location changes
        Query constLLQRef = ref.child("locns").child(ouid).orderByKey();
        constLLQRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                setOtherMarker(ouid, dataSnapshot);
                computeETAs();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[Map Activity] Read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void setOtherMarker(final String ouid, DataSnapshot dataSnapshot) {
        double olat = -12345, olon = -67890;
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            switch (child.getKey()) {
                case "latitude":
                    olat = (double) child.getValue();
                    break;
                case "longitude":
                    olon = (double) child.getValue();
                    break;
            }
        }

        if (olat > -12345 && olon > -12345) {
            String oName = FriendListActivity.names.get(ouid);
            Marker oMarker = others.get(ouid);

            if (oMarker != null) { oMarker.remove(); }
            oMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(olat,
                    olon)).title(oName));
            oMarker.showInfoWindow();
            others.put(ouid, oMarker);
        }
    }

    class Pair {
        String ouid, queryRes;

        public Pair(String ouid, String queryRes) {
            this.ouid = ouid;
            this.queryRes = queryRes;
        }
    }

    class DirQueryTask extends AsyncTask<String, Void, Pair> {
        private Exception exception;

        protected Pair doInBackground(String... urls) {
            StringBuilder stringBuilder = new StringBuilder();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    stringBuilder = new StringBuilder();
                    int b;
                    while ((b = in.read()) != -1) {
                        stringBuilder.append((char) b);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(stringBuilder.toString());
                JSONArray array = jsonObject.getJSONArray("routes");
                JSONObject routes = array.getJSONObject(0);
                JSONArray legs = routes.getJSONArray("legs");
                JSONObject steps = legs.getJSONObject(0);
                JSONObject distance = steps.getJSONObject("distance");
                return new Pair(urls[1], distance.getString("text"));
            } catch (JSONException e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(Pair p) {
            if (this.exception == null) {
                double miDist = Double.parseDouble(p.queryRes.replaceAll("[^0-9\\.]", ""));
                Marker oMarker = others.get(p.ouid);
                if (oMarker == null) { return; }

                oMarker.setSnippet(String.valueOf((int) (miDist / 0.45)) + " min (" + miDist + " mi)");
                // ^TODO: fix this. Currently saving API calls by assuming everyone travels @ 27 mph
            }
        }
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
