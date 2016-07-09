/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vernonsung.testquerygcs;

import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.location.LocationServices;

/**
 * Our main Activity in this sample. Displays a grid of items which an image and title. When the
 * user clicks on an item, {@link ItemDetailFragment} is launched, using the Activity Scene Transitions
 * framework to animatedly do so.
 */
public class MainActivity extends GoogleApiActivity
                       implements ItemListFragment.ItemListFragmentListener,
                                  ItemDetailFragment.ItemDetailFragmentListener,
                                  CreateItemFragment.OnFetchLocationListener {
    private static final String LOG_TAG = "TestGood";

    // Item ID in the startup intent
    public static final String INTENT_PARM_ITEM_ID = "intent_parm_item_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // UI. Show ItemListFragment at the first start. Fragment manager will recreate it after screen orientation changes.
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(R.id.frameMain, new ItemListFragment()).commit();
        }

        // Show item detail if an item ID is given
        String itemId = getIntent().getStringExtra(INTENT_PARM_ITEM_ID);
        if (itemId != null && !itemId.isEmpty()) {
            getFragmentManager().beginTransaction().replace(R.id.frameMain, new ItemListFragment()).commit();
            getFragmentManager().beginTransaction().replace(R.id.frameMain, ItemDetailFragment.newInstance(itemId))
                                                   .addToBackStack(null)
                                                   .commit();
        }

        setOnGooglePlayServiceConnectedListener(new OnGooglePlayServiceConnectedListener() {
            @Override
            public void onGooglePlayServiceConnected() {
                updateLocation();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onGcmRegistrationComplete() {
        super.onGcmRegistrationComplete();
        Fragment fragment = getFragmentManager().findFragmentById(R.id.frameMain);
        if (fragment instanceof ItemListFragment) {
            ItemListFragment itemListFragment = (ItemListFragment)fragment;
            itemListFragment.refresh();
        } else if (fragment instanceof ItemDetailFragment) {
            ItemDetailFragment itemDetailFragment = (ItemDetailFragment)fragment;
            itemDetailFragment.refresh();
        }
    }

    @Override
    protected void onGcmRefresh() {
        super.onGcmRefresh();
        Fragment fragment = getFragmentManager().findFragmentById(R.id.frameMain);
        if (fragment instanceof ItemListFragment) {
            ItemListFragment itemListFragment = (ItemListFragment)fragment;
            itemListFragment.refresh();
        } else if (fragment instanceof ItemDetailFragment) {
            ItemDetailFragment itemDetailFragment = (ItemDetailFragment)fragment;
            itemDetailFragment.refresh();
        }
    }

    // Interface ItemListFragment.ItemListFragmentListener ---------------------------------------
    /**
     * When users press "Create button", show CreateItemFragment
     */
    @Override
    public void onCreateItem() {
        getFragmentManager().beginTransaction()
                .replace(R.id.frameMain, new CreateItemFragment())
                .addToBackStack(null)
                .commit();
    }

    /**
     * When users click on an item, show ItemDetailFragment
     */
    @Override
    public void onShowItemDetail(Item2 item) {
        if (item != null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.frameMain, ItemDetailFragment.newInstance(item.getId()))
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * When users want to call APP server API, they need Google Instance ID
     */
    @Override
    public String onGetInstanceId() {
        if (!isRegisteredToAppServer()) {
            return null;
        }
        return InstanceID.getInstance(this).getId();
    }

    /**
     * When users are going send a new item to the server, fetch the latest location for the item.
     * @return the latest location
     */
    @Override
    public Location onFetchLocation() {
        return fetchLocation();
    }

    // Get current location
    private Location fetchLocation() {
        // Make sure Google play service is connected in order to get location from it
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            Log.d(LOG_TAG, getString(R.string.get_location_failed_because_google_play_service_is_not_installed));
            Toast.makeText(this, getString(R.string.get_location_failed_because_google_play_service_is_not_installed), Toast.LENGTH_LONG).show();
            return null;
        }
        // Get location from Google play service
        Location here = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (here == null) {
            Log.d(LOG_TAG, getString(R.string.get_location_failed_because_gps_is_off));
            Toast.makeText(this, getString(R.string.get_location_failed_because_gps_is_off), Toast.LENGTH_LONG).show();
            return null;
        }
        // Vernon debug
        Log.d(LOG_TAG, here.toString());
        return here;
    }

    /**
     * Update location according to different fragments
     */
    private void updateLocation() {
        // Current location
        Location here = fetchLocation();
        if (here == null) {
            Log.d(LOG_TAG, "Wait for initializing Google play service to update location");
            return;
        }

        // Notify fragments to update location
        Fragment fragment = getFragmentManager().findFragmentById(R.id.frameMain);
        if (fragment instanceof ItemListFragment) {
            ItemListFragment itemListFragment = (ItemListFragment)fragment;
            itemListFragment.setHere(here);
        }
    }
}
