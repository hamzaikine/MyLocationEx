package io.github.hamzaikine.mylocation;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.tasks.OnSuccessListener;


import static io.github.hamzaikine.mylocation.FetchAddressIntentService.ADDRESS_KEY;
import static io.github.hamzaikine.mylocation.FetchAddressIntentService.RESULT_ADDRESS_KEY;
import static io.github.hamzaikine.mylocation.FetchAddressIntentService.RESULT_CODE;
import static io.github.hamzaikine.mylocation.FetchAddressIntentService.TRANSACTION_DONE;


public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private final int MY_PERMISSIONS_REQUEST_LOCATION = 1;
    private final int REQUEST_CHECK_SETTINGS = 2;
    private EditText LATITUDE;
    private EditText LONGITUDE;
    private EditText Address;
    private final String REQUESTING_LOCATION_UPDATES_KEY = "location_update_key";
    private boolean mRequestingLocationUpdates = false;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    protected Location lastLocation;
    private String addressOutput;



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        super.onSaveInstanceState(outState);
    }


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle != null){
                addressOutput = bundle.getString(RESULT_ADDRESS_KEY);
                int resultCode = bundle.getInt(RESULT_CODE);

                displayAddressOutput();

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
        updateValuesFromBundle(savedInstanceState);


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


                    LATITUDE.setText("Latitude: " + latitude.toString());
                    LONGITUDE.setText("Longitude: " + longitude.toString());
                }
            }

            ;
        };

        getCurrentLocation();
        startLocationUpdates();


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
        startLocationUpdates();

    }

    private void getCurrentLocation() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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

                    LATITUDE.setText("Latitude: " + latitude.toString());
                    LONGITUDE.setText(" Longitude: " + longitude.toString());
                    Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?z=21");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

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

        // ...

        // Update UI to match restored state
//        updateUI();
    }


    private void startLocationUpdates() {

        checkLocationPermission();

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */);

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

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(ADDRESS_KEY, lastLocation);
        startService(intent);
    }


    private void displayAddressOutput(){
        Address.setText(addressOutput);
    }

    public void showOnMap(View view) {

        // Logic to handle location object
        Double longitude = lastLocation.getLongitude();
        Double latitude = lastLocation.getLatitude();


        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?z=21");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        if(mapIntent.resolveActivity(getPackageManager()) != null){
                     startActivity(mapIntent);
                    }

    }
}
