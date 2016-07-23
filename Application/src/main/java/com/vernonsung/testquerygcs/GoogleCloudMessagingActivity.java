package com.vernonsung.testquerygcs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

// Provide Google Cloud Messaging related utility for an activity to use
public class GoogleCloudMessagingActivity extends AppCompatActivity {
    // Components
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    // Constants
    private static final String LOG_TAG = "TestGood";
    // Time to resolve Google Play service unavailability
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Receive token
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Identify action
                String action = intent.getAction();
                switch (action) {
                    case MyConstants.REGISTRATION_COMPLETE:
                        onGcmRegistrationComplete();
                        break;
                    case MyConstants.UNREGISTRATION_COMPLETE:
                        onGcmUnregistrationComplete();
                        break;
                    case MyConstants.SUBSCRIPTION_COMPLETE:
                        Toast.makeText(getApplicationContext(), "Subscribe topic successfully", Toast.LENGTH_SHORT).show();
                        break;
                    case MyConstants.UNSUBSCRIBING_COMPLETE:
                        Toast.makeText(getApplicationContext(), "Unsubscribe topic successfully", Toast.LENGTH_SHORT).show();
                        break;
                    case MyConstants.REFRESH:
                        onGcmRefresh();
                        break;
                    default:
                        Log.d(LOG_TAG, "Main activity received an unrecognized action");
                }
            }
        };
        // Fetch a token and register to the APP server when the APP starts
        if (savedInstanceState == null) {
            setUnregisteredToAppServer();
            fetchToken();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(MyConstants.REGISTRATION_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(MyConstants.UNREGISTRATION_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(MyConstants.SUBSCRIPTION_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(MyConstants.UNSUBSCRIBING_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(MyConstants.REFRESH));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    protected void fetchToken() {
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            intent.setAction(MyConstants.ACTION_GET_TOKEN);
            startService(intent);
        }
    }

    protected void showToken() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String token = sharedPreferences.getString(MyConstants.REGISTRATION_TOKEN, "deleted");
        Log.d(LOG_TAG, "Token is " + token);
        Toast.makeText(this, getString(R.string.server_is_connected), Toast.LENGTH_SHORT).show();
    }

    // Trigger when a broadcast intent with action REGISTRATION_COMPLETE is received
    protected void onGcmRegistrationComplete() {
        if (!isRegisteredToAppServer()) {
            finish();
        } else {
            showToken();
        }
    }

    // Trigger when a broadcast intent with action UNREGISTRATION_COMPLETE is received
    protected void onGcmUnregistrationComplete() {
        showToken();
    }

    // Trigger when a broadcast intent with action REFRESH is received
    protected void onGcmRefresh() {
    }

    public void subscribeTopic(String topic) {
        if (topic.isEmpty()) {
            Toast.makeText(this, "Please give a topic name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check Google Cloud Messaging registration
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = sharedPreferences.getString(MyConstants.REGISTRATION_TOKEN, "");
        Boolean flagSent = sharedPreferences.getBoolean(MyConstants.SENT_TOKEN_TO_SERVER, false);

        if (token.isEmpty() || !flagSent) {
            // Server hasn't received registration token yet
            Log.d(LOG_TAG, "Server hasn't received registration token yet, it needn't unregister");
            Toast.makeText(this, "Server hasn't received registration token yet, it needn't unregister", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start the service to tell the server
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            intent.setAction(MyConstants.ACTION_SUBSCRIBE_TOPIC);
            intent.putExtra(MyConstants.TOPIC, topic);
            startService(intent);
        }
    }

    public void unsubscribeTopic(String topic) {
        if (topic.isEmpty()) {
            Toast.makeText(this, "Please give a topic name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check Google Cloud Messaging registration
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = sharedPreferences.getString(MyConstants.REGISTRATION_TOKEN, "");
        Boolean flagSent = sharedPreferences.getBoolean(MyConstants.SENT_TOKEN_TO_SERVER, false);

        if (token.isEmpty() || !flagSent) {
            // Server hasn't received registration token yet
            Log.d(LOG_TAG, "Server hasn't received registration token yet, it needn't unregister");
            Toast.makeText(this, "Server hasn't received registration token yet, it needn't unregister", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start the service to tell the server
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            intent.setAction(MyConstants.ACTION_UNSUBSCRIBE_TOPIC);
            intent.putExtra(MyConstants.TOPIC, topic);
            startService(intent);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(LOG_TAG, "This device does not support Google Play Services.");
                Toast.makeText(this, "This device does not support Google Play Services.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Set an unregistered flag to indicate that it hasn't registered to the APP company server since this time APP started.
     */
    protected void setUnregisteredToAppServer() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(MyConstants.SENT_TOKEN_TO_SERVER, false).apply();
    }

    /**
     * Check whether it has registered to the APP company server since this time APP started.
     * @return APP has registered or not.
     */
    protected boolean isRegisteredToAppServer() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(MyConstants.SENT_TOKEN_TO_SERVER, false);
    }
}
