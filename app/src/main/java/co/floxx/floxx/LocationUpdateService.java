package co.floxx.floxx;

import android.Manifest;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * An IntentService subclass for sending asynchronous location updates to Firebase
 * on a separate handler thread.
 * @author owenjow
 */
public class LocationUpdateService extends JobService implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static Firebase ref;
    private static GoogleApiClient mGoogleApiClient;
    private static LocationRequest mLocationRequest;
    private static String uid;
    private static boolean isMakingRequests;
    public static final String TAG = LocationUpdateService.class.getSimpleName();

    /**
     * Starts a service to continually update user UID's Firebase location. If
     * the service is already performing a task this action will be queued.
     */
    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle extras = params.getExtras();
        isMakingRequests = false;

        Firebase.setAndroidContext(this);
        ref = new Firebase("https://floxx.firebaseio.com/");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1000);
        LocationUpdateService.uid = (String) extras.get("uid");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Try to connect mGoogleApiClient
        for (int i = 0; i < 3; i++) {
            try {
                mGoogleApiClient.connect();
                if (isMakingRequests || mGoogleApiClient.isConnected()) {
                    break;
                }

                Thread.sleep(3500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!isMakingRequests && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            isMakingRequests = true;
        }

        return true; // supposedly I need to call jobFinished
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || isMakingRequests) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        isMakingRequests = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Location services connection failed with code "
                + connectionResult.getErrorCode());
    }

    /**
     * Publishes a location update to Firebase
     * (https://floxx.firebaseio.com/) for the given user.
     */
    @Override
    public void onLocationChanged(Location location) {
        Utility.saveLatLng(ref, uid, location.getLatitude(), location.getLongitude());
    }
}
