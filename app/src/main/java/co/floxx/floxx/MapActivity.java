package co.floxx.floxx;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.BitmapDescriptor;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    private HashMap<String, Marker> others = new HashMap<String, Marker>(); // ouid -> oMarker
    private String meetupId = "";
    private String markerColor = Utility.BLUE_HEX;
    private HashMap<String, Integer> etas = new HashMap<String, Integer>();
    private HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
    private boolean leaveButtonExists;
    private final static int M_JOB_ID = 21;
    private int hiddenDisplayCount;
    private HashMap<String, String> markerColors = new HashMap<String, String>();
    private String uid;
    private Button selectedTransportButton;
    private String currTransportMode = "driving";
    private boolean etaExists;

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
    // - onBackPressed
    //================================================================================

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        // Unpack intent extras (user collection + meetup ID)
        for (String ouid : intent.getExtras().keySet()) {
            if (!ouid.equals("meetup id")) {
                others.put(ouid, null);
                setPermissionListener(ouid);
                permissions.put(ouid, false); // just for you Tony
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

        Typeface montserrat = Typeface.createFromAsset(getAssets(), "Montserrat-Regular.otf");
        ((TextView) mContentView).setTypeface(montserrat);

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
                .setFastestInterval(1000); // 1 second, in milliseconds

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

        uid = ref.getAuth().getUid();

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
                        setPermissionListener(ouid);
                        permissions.put(ouid, false);

                        setOtherMarkerFull(ouid, ref);
                        setETAListener(ouid, ref);
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError e) {
                System.out.println("Read failed: " + e.getMessage());
            }
        });

        // Try and get the user's marker color
        Query queryRef = ref.child("locns").child(uid);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Double color = (Double) snapshot.child("color").getValue();
                if (color != null) {
                    markerColor = "#" + Integer.toHexString(color.intValue());
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[getMarkerColor] Read failed: " + firebaseError.getMessage());
            }
        });

        // TODO (OJ?): If this works, we should probably migrate all locn updates to this service
        ComponentName mServiceComponent = new ComponentName(this, LocationUpdateService.class);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("uid", uid);

        JobInfo locnUpdateTask = new JobInfo.Builder(M_JOB_ID, mServiceComponent)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(bundle)
                .build();
        JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(locnUpdateTask);

        addTransportOptionsToUI();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap != null) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            mMap.setPadding(0, 0, 0, findViewById(R.id.rectangleView).getHeight());

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

        if (!etaExists) {
            TextView etatv = (TextView) findViewById(R.id.eta_text);
            etatv.setText("Computing ETA. Please wait...");
        }

        computeETA();

        mMap.setOnMapClickListener(null); // we only want to be adding one marker
        saveMeetupLocation(point.latitude, point.longitude);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
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
        setUpMapIfNeeded();

        Log.i(TAG, "Location services connected.");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_REQ_CODE);
            return;
        }

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intermediary.mapToPortalOthers = new HashSet<String>(others.keySet());
        Intermediary.mapToPortalMeetupId = meetupId;
    }

    //================================================================================
    // Setup methods
    //================================================================================
    // - setUpMap
    // - setUpMapIfNeeded
    // - attachBaseContext
    // - addTransportOptionsToUI (adds buttons for different transport modes)
    //================================================================================

    private void setUpMap() {
        if (mLastLocation != null) {
            LocationManager locationManager =
                    (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            try {
                Location location =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (marker != null) { marker.remove(); }
                marker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),
                        location.getLongitude())).title("You").icon(getBitmapDescriptor(markerColor)));
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
                setUpMap();
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * Transport selection, featuring lots of well-written and non-repetitive code.
     */
    private void addTransportOptionsToUI() {
        // Create the four buttons
        final Button drivingButton = new Button(this);
        final Button bicyclingButton = new Button(this);
        final Button transitButton = new Button(this);
        final Button walkingButton = new Button(this);

        drivingButton.setBackgroundResource(R.drawable.driving);
        bicyclingButton.setBackgroundResource(R.drawable.bicycling);
        transitButton.setBackgroundResource(R.drawable.transit);
        walkingButton.setBackgroundResource(R.drawable.walking);

        drivingButton.setId(R.id.driving);
        bicyclingButton.setId(R.id.bicycling);
        transitButton.setId(R.id.transit);
        walkingButton.setId(R.id.walking);

        // Set button listeners
        drivingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTransportMode(drivingButton, "driving");
            }
        });
        bicyclingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTransportMode(bicyclingButton, "bicycling");
            }
        });
        transitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTransportMode(transitButton, "transit");
            }
        });
        walkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTransportMode(walkingButton, "walking");
            }
        });

        // Params for each of the buttons
        RelativeLayout.LayoutParams drivingParams = getTransportParams();

        RelativeLayout.LayoutParams bicyclingParams = getTransportParams();
        bicyclingParams.addRule(RelativeLayout.RIGHT_OF, R.id.driving);

        RelativeLayout.LayoutParams transitParams = getTransportParams();
        transitParams.addRule(RelativeLayout.RIGHT_OF, R.id.bicycling);

        RelativeLayout.LayoutParams walkingParams = getTransportParams();
        walkingParams.addRule(RelativeLayout.RIGHT_OF, R.id.transit);

        // Add the buttons to the layout
        RelativeLayout buttonContainer = (RelativeLayout) findViewById(R.id.transport_modes);
        buttonContainer.addView(drivingButton, drivingParams);
        buttonContainer.addView(bicyclingButton, bicyclingParams);
        buttonContainer.addView(transitButton, transitParams);
        buttonContainer.addView(walkingButton, walkingParams);

        bicyclingButton.getBackground().setColorFilter(null);
        transitButton.getBackground().setColorFilter(null);
        walkingButton.getBackground().setColorFilter(null);

        // Select the driving button (this is the default)
        drivingButton.getBackground().setColorFilter(new LightingColorFilter(0xff888888, 0x000000));
        selectedTransportButton = drivingButton;
    }

    //================================================================================
    // Utility/update classes and methods
    //================================================================================
    // - handleNewLocation (what to do when we have a location update for the user)
    // - saveMeetupLocation (saves location to Firebase so other users can access it)
    // - updateMeetingMarker (updates the meeting marker position)
    // - computeETA (computes ETA and distance information for you, yourself, and you)
    // - setOtherMarkerFull (creates a listener whose callback = setOtherMarker)
    // - setOtherMarker (places a marker for a user that isn't on the map already)
    // - Pair (POJO that serves as a ouid/queryRes data bundle)
    // - DirQueryTask (asynchronous task that queries the Directions API)
    // - handleNewETA (updates the UI when it receives a new ETA information packet)
    // - toggle
    // - hide
    // - show
    // - delayedHide
    // - getBitmapDescriptor (gets a color from a hex string)
    // - setPermissionListener (sets a listener for some user's location permissions)
    // - updateHiddenUsersView [adds to (or removes from) the GUI elt for hidden users]
    // - updateTransportMode (assigns a new transport mode and recomputes ETAs)
    // - setETAListener (sets an ETA listener for someone else)
    // - getTransportParams
    //================================================================================

    public void handleNewLocation(Location location) {
        Firebase ref = new Firebase("https://floxx.firebaseio.com/");
        Utility.saveLatLng(ref, uid, location.getLatitude(), location.getLongitude());

        Log.d(TAG, location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        MarkerOptions options = new MarkerOptions()
                .position(latLng).title("You").icon(getBitmapDescriptor(markerColor));
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

            if (!etaExists) {
                TextView etatv = (TextView) findViewById(R.id.eta_text);
                String etaMsg = "Computing ETA. Please wait...";
                etatv.setText(etaMsg);
            }
        } else {
            meetingMarker.setPosition(new LatLng(latitude, longitude));
        }

        computeETA();
    }

    private void computeETA() {
        if (meetingMarker != null && marker != null) {
            LatLng meetingPos = meetingMarker.getPosition();
            LatLng yourPosition = marker.getPosition();

            // TODO (Owen): add API key and request "Maps-official" travel time
            // TODO: also integrate transport mode [currTransportMode]
            new DirQueryTask().execute("http://maps.googleapis.com/maps/api/directions/json?origin="
                    + yourPosition.latitude + "," + yourPosition.longitude + "&destination="
                    + meetingPos.latitude + "," + meetingPos.longitude + "&mode=driving&sensor=false",
                    uid);
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
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("[Map Activity] Read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void setOtherMarker(final String ouid, DataSnapshot dataSnapshot) {
        double olat = -Utility.LAT_INF, olon = -Utility.LON_INF, color = -1.0;
        for (DataSnapshot child : dataSnapshot.getChildren()) {
            switch (child.getKey()) {
                case "latitude":
                    olat = (double) child.getValue();
                    break;
                case "longitude":
                    olon = (double) child.getValue();
                    break;
                case "color":
                    color = (double) child.getValue();
                    break;
            }
        }

        if (olat > -Utility.LAT_INF && olon > -Utility.LON_INF) {
            String oName = FriendListActivity.names.get(ouid);
            Marker oMarker = others.get(ouid);
            String hexStr = (color >= 0) ?
                    "#" + Integer.toHexString((int) color) : Utility.BURGUNDY_HEX;
            markerColors.put(ouid, hexStr);

            if (oMarker != null) { oMarker.remove(); }
            if (permissions.get(ouid)) { // some users just want to be hidden
                oMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(olat,
                        olon)).title(oName).icon(getBitmapDescriptor(hexStr)));
                oMarker.showInfoWindow();
                others.put(ouid, oMarker);
            }
        }
    }

    class Pair {
        String id, queryRes;

        public Pair(String id, String queryRes) {
            this.id = id; // by ID, we mean UID
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
                int eta = (int) (miDist / 0.45);
                // ^TODO: fix this. Currently saving API calls by assuming everyone travels @ 27 mph

                // Publish ETA to database
                ArrayList<Double> etaInfo = new ArrayList<Double>();
                etaInfo.add((double) eta); etaInfo.add(miDist);
                Firebase ref = new Firebase("https://floxx.firebaseio.com/");
                ref.child(meetupId).child(p.id).setValue(etaInfo);

                handleNewETA(p.id, eta, miDist);
            }
        }
    }

    /**
     * Updates the UI and internal configuration to account for some user's new ETA.
     * @param id some user's UID
     * @param eta the ETA for said user
     */
    private void handleNewETA(String id, int eta, double miDist) {
        Marker userMarker = (id.equals(uid)) ? marker : others.get(id);
        if (userMarker != null) {
            userMarker.setSnippet(String.valueOf(eta) + " min (" + miDist + " mi)");
        }

        etas.put(id, eta);

        // Calculate the overall ETA (the maximum of all participants' ETAs)
        int collectiveETA = Collections.max(etas.values());
        String etaMsg = "ETA: " + collectiveETA + " min";

        TextView etatv = (TextView) findViewById(R.id.eta_text);
        if (collectiveETA <= 1) {
            // Consider the meetup effectively "concluded"
            etaMsg += " (...move the marker?)";
            etatv.setText(etaMsg);
            etaExists = true;

            if (!leaveButtonExists) {
                Button leaveButton = new Button(MapActivity.this);
                leaveButton.setText("Leave");

                LinearLayout ll = (LinearLayout) findViewById(R.id.lb_container);
                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                ll.addView(leaveButton, lp);

                leaveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Firebase ref = new Firebase("https://floxx.firebaseio.com/");
                        Utility.leave(meetupId, ref, ref.getAuth().getUid());
                        finish();
                    }
                });
                leaveButtonExists = true;
            }
        } else {
            if (leaveButtonExists) {
                ((LinearLayout) findViewById(R.id.lb_container)).removeAllViews();
                leaveButtonExists = false;
            }

            etatv.setText(etaMsg);
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

    private BitmapDescriptor getBitmapDescriptor(String hexStr) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(hexStr), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }

    private void setPermissionListener(final String uid) {
        final Firebase plRef = new Firebase("https://floxx.firebaseio.com/permissions/" + uid);
        plRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    new Firebase("https://floxx.firebaseio.com/permissions/"
                            + uid + "/location").setValue("on");
                    permissions.put(uid, true);
                    return;
                }

                switch((String) snapshot.child("location").getValue()) {
                    case "on":
                        permissions.put(uid, true);
                        updateHiddenUsersView(uid, false);
                        break;
                    case "off":
                        permissions.put(uid, false);
                        updateHiddenUsersView(uid, true);
                        break;
                }
            }

            @Override
            public void onCancelled(FirebaseError e) {
                System.out.println("[setPermissionListener] Read failed: " + e.getMessage());
            }
        });
    }

    private void updateHiddenUsersView(String uid, boolean isHidden) {
        final LinearLayout ll = (LinearLayout) findViewById(R.id.hidden_users);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        hiddenDisplayCount += (isHidden) ? 1 : -1;
        if (hiddenDisplayCount == 0) {
            // Hide the hidden users display
            ScrollView sv = (ScrollView) findViewById(R.id.hidden_rect);
            sv.getLayoutParams().width = 0;
        } else if (hiddenDisplayCount == 1 && isHidden) {
            // Show the hidden users display
            ScrollView sv = (ScrollView) findViewById(R.id.hidden_rect);
            sv.getLayoutParams().width =
                    ScrollView.LayoutParams.WRAP_CONTENT;
        }

        if (isHidden) {
            String username = FriendListActivity.names.get(uid);
            TextView tv = new TextView(this);
            tv.setId(uid.hashCode());
            tv.setText(username);

            String colorHex = markerColors.get(uid);
            if (colorHex != null) {
                tv.setTextColor(Color.parseColor(colorHex));
            }

            ll.addView(tv, lp);
        } else {
            final TextView tv = (TextView) findViewById(uid.hashCode());

            // Get that thing out of here
            ll.post(new Runnable() {
                public void run() {
                    ll.removeView(tv);
                }
            });
        }
    }

    private void updateTransportMode(Button button, String transportMode) {
        selectedTransportButton.getBackground().setColorFilter(null);
        button.getBackground().setColorFilter(new LightingColorFilter(0xff888888, 0x000000));
        selectedTransportButton = button;
        currTransportMode = transportMode;
        computeETA();
    }

    /**
     * Assigns an ETA listener for some other guy.
     * @param ouid the other guy's user ID
     */
    private void setETAListener(final String ouid, Firebase ref) {
        Query etaRef = ref.child(meetupId).child(ouid);
        etaRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Double> etaInfo = (ArrayList<Double>) dataSnapshot.getValue();
                if (etaInfo != null) {
                    handleNewETA(ouid, etaInfo.get(0).intValue(), etaInfo.get(1));
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.w("MA – setETAListener", "Read failed: " + firebaseError.getMessage());
            }
        });
    }

    private RelativeLayout.LayoutParams getTransportParams() {
        int pxLen = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                getResources().getDisplayMetrics());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pxLen, pxLen);
        params.setMargins(8, 8, 8, 0); // (left, top, right, bottom)
        return params;
    }
}
