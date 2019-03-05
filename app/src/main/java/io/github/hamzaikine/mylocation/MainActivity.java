package io.github.hamzaikine.mylocation;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;

import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;


import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

import static io.github.hamzaikine.mylocation.FetchAddressIntentService.ADDRESS_KEY;
import static io.github.hamzaikine.mylocation.FetchAddressIntentService.RESULT_ADDRESS_KEY;
import static io.github.hamzaikine.mylocation.FetchAddressIntentService.RESULT_CODE;
import static io.github.hamzaikine.mylocation.FetchAddressIntentService.TRANSACTION_DONE;


public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private EditText LATITUDE;
    private EditText LONGITUDE;
    private EditText Address;
    private final String REQUESTING_LOCATION_UPDATES_KEY = "location_update_key";
    private boolean mRequestingLocationUpdates;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    protected Location lastLocation;
    private String addressOutput;
    private final String LOCATION_KEY = "location_key";
    private String mLastUpdateTime;
    private SettingsClient mSettingsClient;

    private static final int REQUEST_CHECK_SETTINGS = 2;
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                addressOutput = bundle.getString(RESULT_ADDRESS_KEY);
                int resultCode = bundle.getInt(RESULT_CODE);

                Address.setText(addressOutput);

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LATITUDE = findViewById(R.id.latitude);
        LONGITUDE = findViewById(R.id.longitude);
        Address = findViewById(R.id.address);
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        updateValuesFromBundle(savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);


        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();


    }

    private void checkLocationPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage(R.string.dialog_message)
                        .setTitle(R.string.dialog_title);

                AlertDialog dialog = builder.create();

                dialog.show();

            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            }
            return;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // Location task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void getLocation(View view) {

        getCurrentLocation();

    }

    private void getCurrentLocation() {


        checkLocationPermission();

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {

                    //current location
                    lastLocation = location;

                    if (!Geocoder.isPresent()) {
                        Toast.makeText(MainActivity.this,
                                R.string.no_geocoder_available,
                                Toast.LENGTH_LONG).show();
                        return;
                    }


                    // Logic to handle location object
                    Double longitude = location.getLongitude();
                    Double latitude = location.getLatitude();

                    mRequestingLocationUpdates = true;

                    startLocationUpdates();

                    LATITUDE.setText("Latitude: " + latitude.toString());
                    LONGITUDE.setText("Longitude: " + longitude.toString());

                    Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?z=21");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    //fetch the address
                    startIntentService();


                }
            }
        });

    }


    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        // Update the value of requestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }

        if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
            lastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
        }


        // ...

        // Update UI to match restored state
//        updateUI();
    }


    private void startLocationUpdates() {


        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i("Location_Satisfied", "All location settings are satisfied.");

                        checkLocationPermission();

                        //noinspection MissingPermission
                        fusedLocationClient.requestLocationUpdates(locationRequest,
                                locationCallback, Looper.myLooper());


                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i("Location_Not_Satisfied", "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i("PendingIntent", "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e("ErrorMessage", errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }


                    }
                });
    }


    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(TRANSACTION_DONE));
        //check whether location updates are currently active, and activate them if not
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        stopLocationUpdates();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        outState.putParcelable(LOCATION_KEY, lastLocation);
        super.onSaveInstanceState(outState);
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d("stopLocationUpdates", "stopLocationUpdates: updates never requested.");
            return;
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(ADDRESS_KEY, lastLocation);
        startService(intent);
    }

    public void showOnMap(View view) {

        // Logic to handle location object
        Double longitude = lastLocation.getLongitude();
        Double latitude = lastLocation.getLatitude();


        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?z=21");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }

    }


    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    Double longitude = location.getLongitude();
                    Double latitude = location.getLatitude();

                    Log.d("LocationCallback", "we are getting new location" + location.toString());

                    //update last location so it reflects the new address.
                    lastLocation = location;

                    LATITUDE.setText("Latitude: " + latitude.toString());
                    LONGITUDE.setText("Longitude: " + longitude.toString());

                    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                    Toast.makeText(getApplicationContext(), "lastTimeUpdate: " + mLastUpdateTime, Toast.LENGTH_LONG).show();

                    Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?z=21");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                    //fetch the address
                    startIntentService();
                }
            }

            ;
        };

    }


    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        mLocationSettingsRequest = builder.build();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("RESULT_OK_TAG", "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("RESULT_CANCELED_TAG", "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }


}
