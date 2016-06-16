package co.floxx.floxx;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.firebase.client.Firebase;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;

/**
 * An IntentService subclass for sending asynchronous location updates to Firebase
 * on a separate handler thread.
 * @author owenjow
 */
public class LocationUpdateService extends IntentService implements LocationListener {
    private static final String START_LOCN_UPDATES = "co.floxx.floxx.action.SLU";
    private static Firebase ref;
    private static GoogleApiClient mGoogleApiClient;
    private static LocationRequest mLocationRequest;
    private static String uid;

    public LocationUpdateService() {
        super("LocationUpdateService");
    }

    /**
     * Starts a service to continually update user UID's Firebase location. If
     * the service is already performing a task this action will be queued.
     */
    public static void startUpdates(Context context, String uid,
            GoogleApiClient mGoogleApiClient, LocationRequest mLocationRequest) {
        ref = new Firebase("https://floxx.firebaseio.com/");
        LocationUpdateService.mGoogleApiClient = mGoogleApiClient;
        LocationUpdateService.mLocationRequest = mLocationRequest;
        LocationUpdateService.uid = uid;

        Intent intent = new Intent(context, LocationUpdateService.class);
        intent.setAction(START_LOCN_UPDATES);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (START_LOCN_UPDATES.equals(action)) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                // Try to connect mGoogleApiClient
                for (int i = 0; i < 3; i++) {
                    try {
                        mGoogleApiClient.connect();
                        if (mGoogleApiClient.isConnected()) {
                            break;
                        }

                        Thread.sleep(3500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (mGoogleApiClient.isConnected()) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleApiClient, mLocationRequest, this);
                }
            }
        }
    }

    /**
     * Publishes a location update to Firebase
     * (https://floxx.firebaseio.com/) for the given user.
     */
    @Override
    public void onLocationChanged(Location location) {
        Map<String, Double> map = new HashMap<String, Double>();
        map.put("latitude", location.getLatitude());
        map.put("longitude", location.getLongitude());

        ref.child("locns").child(uid).setValue(map);
    }
}
