package io.github.hamzaikine.mylocation;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;


public class FetchAddressIntentService extends IntentService {



        private int result = Activity.RESULT_CANCELED;
        public static final String PACKAGE_NAME =
                "io.github.hamzaikine.mylocation";
        public static final String RESULT_ADDRESS_KEY = PACKAGE_NAME +
                ".RESULT_ADDRESS_KEY";
        public static final String TRANSACTION_DONE = PACKAGE_NAME +
                ".TRANSACTION_DONE";
        public static final String RESULT_CODE = PACKAGE_NAME + ".RESULT_CODE";
    public static final String ADDRESS_KEY = PACKAGE_NAME +
            ".ADDRESS_KEY";

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        //get a geocoder from Locale
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());



            if (intent != null) {
                String errorMessage = "";
                // Get the location passed to this service through an extra.
                Location location = intent.getParcelableExtra(
                        ADDRESS_KEY);

                List<Address> addresses = null;

                try {
                    addresses = geocoder.getFromLocation(location.getLatitude(),
                            location.getLongitude(),
                            1);
                } catch (IOException e) {
                    errorMessage = getString(R.string.service_not_available);
                } catch (IllegalArgumentException illegalArgumentException) {
                    // Catch invalid latitude or longitude values.
                    errorMessage = getString(R.string.invalid_lat_long_used);
                    Log.e(TAG, errorMessage + ". " +
                            "Latitude = " + location.getLatitude() +
                            ", Longitude = " +
                            location.getLongitude(), illegalArgumentException);
                }

                // Handle case where no address was found.
                if (addresses == null || addresses.size() == 0) {
                    if (errorMessage.isEmpty()) {
                        errorMessage = getString(R.string.no_address_found);
                        Log.e(TAG, errorMessage);
                    }
                    deliverResultToReceiver(result, errorMessage);
                } else {
                    Address address = addresses.get(0);
                    ArrayList<String> addressFragments = new ArrayList<String>();

                    // Fetch the address lines using getAddressLine,
                    // join them, and send them to the thread.
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        addressFragments.add(address.getAddressLine(i));
                    }
                    Log.i(TAG, getString(R.string.address_found));

                    // successfully finished
                    result = Activity.RESULT_OK;
                    deliverResultToReceiver(result,
                            TextUtils.join(System.getProperty("line.separator"),
                                    addressFragments));
                }
            }



    }

    private void deliverResultToReceiver(int resultCode, String message) {
       Intent intent = new Intent(TRANSACTION_DONE);
       intent.putExtra(RESULT_ADDRESS_KEY,message);
       intent.putExtra(RESULT_CODE,resultCode);
       sendBroadcast(intent);
    }


}
