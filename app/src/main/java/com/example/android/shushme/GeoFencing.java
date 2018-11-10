package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

public class GeoFencing implements ResultCallback<Status> {

    private static final String TAG = GeoFencing.class.getSimpleName();
    private static final long GEOFENCE_TIMEOUT = (int) java.util.concurrent.TimeUnit.HOURS.toMillis(24);
    private static final float GEOFENCE_RADIUS = 50;
    private static final int GEOFENCE_PENDING_INTENT_REQUEST_CODE = 0;

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private PendingIntent geofencePendingIntent;
    private List<Geofence> mGeoFenceList;

    public GeoFencing(Context mContext, GoogleApiClient mGoogleApiClient) {
        this.mContext = mContext;
        this.mGoogleApiClient = mGoogleApiClient;
        this.geofencePendingIntent = null;
        this.mGeoFenceList = new ArrayList<>();
    }

    private GoogleApiClient getmGoogleApiClient() {
        return mGoogleApiClient;
    }

    private PendingIntent getGeofencePendingIntent() {

        if (geofencePendingIntent != null)
            return geofencePendingIntent;

        Intent intent = new Intent(mContext, GeoFencingBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, GEOFENCE_PENDING_INTENT_REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private List<Geofence> getmGeoFenceList() {
        return mGeoFenceList;
    }

    private GeofencingRequest getGeoFencingRequest() {

        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(getmGeoFenceList())
                .build();
    }

    public void registerGeofences() {

        if (getmGoogleApiClient() == null || !getmGoogleApiClient().isConnected() || getGeofencePendingIntent() == null || getmGeoFenceList().isEmpty())
            return;

        try {
            LocationServices.GeofencingApi.addGeofences(
                    getmGoogleApiClient(),
                    getGeoFencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException ex) {
            Log.d(TAG, String.format("Error: %s", ex.getMessage()));
        }
    }

    public void unregisterGeofences() {

        if (getmGoogleApiClient() == null || !getmGoogleApiClient().isConnected() || getGeofencePendingIntent() == null)
            return;

        try {
            LocationServices.GeofencingApi.removeGeofences(
                    getmGoogleApiClient(),
                    geofencePendingIntent
            ).setResultCallback(this);
        } catch (SecurityException ex) {
            Log.d(TAG, String.format("Error: %s", ex.getMessage()));
        }
    }

    public void updateGeoFenceList(PlaceBuffer placeBuffer) {

        for (Place place : placeBuffer) {
            //A Daily Job Scheduler is needed to daily register the Geofences because they expire in 24hours
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(place.getId())
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(place.getLatLng().latitude, place.getLatLng().longitude, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            mGeoFenceList.add(geofence);
        }
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.d(TAG, String.format("Error adding/removing Geofences: %s.", status.getStatus()));
    }
}
