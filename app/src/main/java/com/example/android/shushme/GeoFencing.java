package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

public class GeoFencing {

    private static final long GEOFENCE_TIMEOUT = (int) java.util.concurrent.TimeUnit.HOURS.toMillis(24);
    private static final float GEOFENCE_RADIUS = 50;

    private Context mContext;
    private GoogleApiClient mClient;
    private PendingIntent pendingIntent;
    private List<Geofence> mGeoFenceList;

    public GeoFencing(Context mContext, GoogleApiClient mClient) {
        this.mContext = mContext;
        this.mClient = mClient;
        this.pendingIntent = null;
        this.mGeoFenceList = new ArrayList<>();
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
}
