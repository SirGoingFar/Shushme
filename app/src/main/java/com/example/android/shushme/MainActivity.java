package com.example.android.shushme;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener {

    // Constants
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private static final int PLACE_PICKER_REQUEST = 0;

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private GoogleApiClient mClient;
    private List<String> allPlacesId;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);


        // Build up the LocationServices API client
        // Uses the addApi method to request the LocationServices API
        // Also uses enableAutoManage to automatically when to connect/suspend the client
        mClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, this)
                .build();

    }

    /***
     * Called when the Google API Client is successfully connected
     *
     * @param connectionHint Bundle of data provided to clients by Google Play services
     */
    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        Log.i(TAG, "API Client Connection Successful!");
        refreshPlaceData();
    }

    /***
     * Called when the Google API Client is suspended
     *
     * @param cause cause The reason for the disconnection. Defined by constants CAUSE_*.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "API Client Connection Suspended!");
    }

    /***
     * Called when the Google API Client failed to connect to Google Play Services
     *
     * @param result A ConnectionResult that can be used for resolving the error
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, "API Client Connection Failed!");
    }

    /***
     * Button Click event handler to handle clicking the "Add new location" Button
     *
     * @param view
     */
    public void onAddPlaceButtonClicked(View view) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.need_location_permission_message), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Intent pickerIntent = new PlacePicker.IntentBuilder().build(this);
            startActivityForResult(pickerIntent, PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
            Log.i(TAG, String.format("GooglePlayServices not available [%s]", e.getMessage()));
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
            Log.i(TAG, String.format("GooglePlayServices not available [%s]", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize location permissions checkbox
        CheckBox locationPermissions = (CheckBox) findViewById(R.id.location_permission_checkbox);
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.setChecked(false);
        } else {
            locationPermissions.setChecked(true);
            locationPermissions.setEnabled(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);

            if (place != null) {
                String placeId = place.getId();
                String placeName = place.getName().toString();
                String placeAddress = place.getAddress().toString();

                //save Place Id to Db
                ContentValues value = new ContentValues();
                value.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeId);

                getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, value);

                refreshPlaceData();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onLocationPermissionClicked(View view) {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_FINE_LOCATION);
    }


    private void refreshPlaceData() {

        List<String> placesIdList = getAllPlacesId();

        if (placesIdList.size() <= 0)
            Toast.makeText(this, "No places selected, please pick one", Toast.LENGTH_SHORT).show();

        Places.GeoDataApi.getPlaceById(mClient, placesIdList.toArray(new String[placesIdList.size()]))
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(@NonNull PlaceBuffer places) {
                        mAdapter.swapDataSource(places);
                    }
                });

        /*PendingResult<PlacePhotoMetadataResult> a = Places.GeoDataApi.getPlacePhotos(mClient, "A_PLACE_ID");
        PlacePhotoMetadataResult s = new Object();
                s.getPhotoMetadata().get(2).getPhoto().setResultCallback(new ResultCallback<PlacePhotoResult>() {
                    @Override
                    public void onResult(@NonNull PlacePhotoResult placePhotoResult) {
                        Bitmap placeImage = placePhotoResult.getBitmap(); //that's the place image
                    }
                });*/
    }

    public List<String> getAllPlacesId() {

        List<String> placesIdList = new ArrayList<>();

        //fetxh all the available places Id from the db
        Cursor cursor = getContentResolver().query(PlaceContract.PlaceEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (cursor != null && cursor.getCount() > 0) {

            int placeIdColumnIndex = cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID);

            if (placeIdColumnIndex < 0)
                return placesIdList;

            while (cursor.moveToNext())
                placesIdList.add(cursor.getString(placeIdColumnIndex));
        }

        return placesIdList;
    }
}
