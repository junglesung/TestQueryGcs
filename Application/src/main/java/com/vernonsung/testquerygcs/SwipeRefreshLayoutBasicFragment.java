/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vernonsung.testquerygcs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.iid.InstanceID;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * A basic sample that shows how to use {@link android.support.v4.widget.SwipeRefreshLayout} to add
 * the 'swipe-to-refresh' gesture to a layout. In this sample, SwipeRefreshLayout contains a
 * scrollable {@link android.widget.ListView} as its only child.
 *
 * <p>To provide an accessible way to trigger the refresh, this app also provides a refresh
 * action item.
 *
 * <p>In this sample app, the refresh updates the ListView with a random set of new items.
 */
public class SwipeRefreshLayoutBasicFragment extends Fragment {

    private static final String LOG_TAG = "TestGood";

    // To use Google play service such as Location API
    GoogleApiClient mGoogleApiClient;
    // Current location
    Location here;
    // Item list
    private Item2[] items;
    // Automatically retry when network is OK.
    boolean flagRefreshNeeded;

    // Threads
    private QueryItemTask mQueryItemTask;

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private GridView mGridView;
    private GridAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Notify the system to allow an options menu for this fragment.
        setHasOptionsMenu(true);

        // Initial variables
        mGoogleApiClient = ((GoogleApiActivity)getActivity()).getGoogleApiClient();
        here = null;
        items = null;
        flagRefreshNeeded = true;
        mQueryItemTask = null;
    }

    // BEGIN_INCLUDE (inflate_view)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sample, container, false);

        // Retrieve the SwipeRefreshLayout and ListView instances
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);

        // BEGIN_INCLUDE (change_colors)
        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);
        // END_INCLUDE (change_colors)

        // Setup the GridView and set the adapter
        mGridView = (GridView) view.findViewById(R.id.grid);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showDetail(adapterView, view, i, l);
            }
        });

        return view;
    }
    // END_INCLUDE (inflate_view)

    // BEGIN_INCLUDE (setup_views)
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new GridAdapter();
        mGridView.setAdapter(mAdapter);

        // BEGIN_INCLUDE (setup_refreshlistener)
        /**
         * Implement {@link SwipeRefreshLayout.OnRefreshListener}. When users do the "swipe to
         * refresh" gesture, SwipeRefreshLayout invokes
         * {@link SwipeRefreshLayout.OnRefreshListener#onRefresh onRefresh()}. In
         * {@link SwipeRefreshLayout.OnRefreshListener#onRefresh onRefresh()}, call a method that
         * refreshes the content. Call the same method in response to the Refresh action from the
         * action bar.
         */
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(LOG_TAG, "onRefresh called from SwipeRefreshLayout");

                initiateRefresh();
            }
        });
        // END_INCLUDE (setup_refreshlistener)
    }
    // END_INCLUDE (setup_views)

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

    // BEGIN_INCLUDE (setup_refresh_menu_listener)
    /**
     * Respond to the user's selection of the Refresh action item. Start the SwipeRefreshLayout
     * progress bar, then initiate the background task that refreshes the content.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                Log.i(LOG_TAG, "Refresh menu item selected");

                // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
                if (!mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(true);
                }

                // Start our refresh background task
                initiateRefresh();

                return true;
            case R.id.menu_privacy:
                showPrivacy();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    // END_INCLUDE (setup_refresh_menu_listener)

    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * Called when an item in the {@link android.widget.GridView} is clicked. Here will launch the
     * {@link DetailActivity}, using the Scene Transition animation functionality.
     */
    public void showDetail(AdapterView<?> adapterView, View view, int position, long id) {
        // Get the specified item
        if (position > items.length) {
            Log.e(LOG_TAG, "User click position " + position + " is out of item number " + items.length);
            return;
        }
        Item2 item = items[position];

        // Transform item to JSON
        String itemJson = new Gson().toJson(item);

        // Construct an Intent as normal
        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PARAM_ITEM, itemJson);
        intent.putExtra(DetailActivity.EXTRA_PARAM_ID, item.getId());

            // BEGIN_INCLUDE(start_activity)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            startActivity(intent);
        } else {
            /**
             * Now create an {@link android.app.ActivityOptions} instance using the
             * {@link ActivityOptionsCompat#makeSceneTransitionAnimation(Activity, Pair[])} factory
             * method.
             */
            ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),

                    // Now we provide a list of Pair items which contain the view we can transitioning
                    // from, and the name of the view it is transitioning to, in the launched activity
                    new Pair<View, String>(view.findViewById(R.id.imageview_item),
                            DetailActivity.VIEW_NAME_HEADER_IMAGE),
                    new Pair<View, String>(view.findViewById(R.id.textview_name),
                            DetailActivity.VIEW_NAME_HEADER_TITLE));

            // Now we can start the Activity, providing the activity options as a bundle
            ActivityCompat.startActivity(getActivity(), intent, activityOptions.toBundle());
            // END_INCLUDE(start_activity)
        }
    }

    // Refresh if needed
    public void tryRefresh() {
        if (!mSwipeRefreshLayout.isRefreshing() && flagRefreshNeeded) {
            initiateRefresh();
            flagRefreshNeeded = false;
        }
    }

    // Force refreshing if it's not refreshing
    public void forceRefresh() {
        if (!mSwipeRefreshLayout.isRefreshing()) {
            initiateRefresh();
        }
    }

    // BEGIN_INCLUDE (initiate_refresh)
    /**
     * By abstracting the refresh process to a single method, the app allows both the
     * SwipeGestureLayout onRefresh() method and the Refresh action item to refresh the content.
     */
    private void initiateRefresh() {
        // Check Google Instance ID registration
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean isTokenSentToServer = sharedPreferences.getBoolean(MyConstants.SENT_TOKEN_TO_SERVER, false);
        if (!isTokenSentToServer) {
            Toast.makeText(activity, getString(R.string.app_is_not_registered_please_check_internet_and_retry_later), Toast.LENGTH_LONG).show();
            return;
        }

        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            mSwipeRefreshLayout.setRefreshing(true);
            // Execute querying thread
            mQueryItemTask = new QueryItemTask();
            mQueryItemTask.execute();
        } else {
            Toast.makeText(getActivity(), getString(R.string.no_network_connection_available), Toast.LENGTH_SHORT).show();
        }
    }
    // END_INCLUDE (initiate_refresh)

    // BEGIN_INCLUDE (refresh_complete)
    /**
     * When the AsyncTask finishes, it calls onRefreshComplete(), which updates the data in the
     * ListAdapter and turns off the progress bar.
     */
    private void onRefreshComplete(Item2[] result) {
        items = result;

        // Remove all items from the ListAdapter, and then replace them with the new items
        mAdapter.notifyDataSetChanged();

        // Stop the refreshing indicator
        mSwipeRefreshLayout.setRefreshing(false);
    }
    // END_INCLUDE (refresh_complete)

    public void setHere(Location here) {
        this.here = here;
        // New location
        if (here == null) {
            Log.d(LOG_TAG, "New location is null");
        } else {
            Log.d(LOG_TAG, "New location " +
                    String.valueOf(here.getLatitude()) + "," +
                    String.valueOf(here.getLongitude()));
        }
        // Remove all items from the ListAdapter, and then replace them with the new items
        mAdapter.notifyDataSetChanged();
    }

    // Show privacy policy
    private void showPrivacy () {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MyConstants.PRIVACY_URL)));
    }

    // Get the latest items from the server in background
    private class QueryItemTask extends AsyncTask<Void, Void, Item2[]> {
        static final String QUERY_ITEM_URL = "https://aliza-1148.appspot.com/api/0.1/items";
        // Screen orientation. Save and disable screen rotation in order to prevent screen rotation destroying the activity and the AsyncTask.
        private int screenOrientation;

        @Override
        protected void onPreExecute() {
            // Disable screen rotation
            Activity activity = getActivity();
            if (activity == null) {
                Log.e(LOG_TAG, "Activity = null");
                return;
            }
            screenOrientation = activity.getRequestedOrientation();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        @Override
        protected Item2[] doInBackground(Void... params) {
            return getItems();
        }

        @Override
        protected void onPostExecute(Item2[] result) {
            super.onPostExecute(result);

            // Tell the Fragment that the refresh has completed
            onRefreshComplete(result);

            // Enable screen rotation
            Activity activity = getActivity();
            if (activity == null) {
                Log.e(LOG_TAG, "Activity = null");
                return;
            }
            activity.setRequestedOrientation(screenOrientation);
        }

        // Send GET to the server
        // Return JSON string of items
        // Return null if failed
        private Item2[] getItems() {
            URL url;
            HttpsURLConnection urlConnection = null;
            Item2[] items = null;

            try {
                url = new URL(QUERY_ITEM_URL);
                urlConnection = (HttpsURLConnection) url.openConnection();

                // Set authentication instance ID
                urlConnection.setRequestProperty(MyConstants.HTTP_HEADER_INSTANCE_ID, InstanceID.getInstance(getActivity()).getId());
                // Set content type
                urlConnection.setRequestProperty("Content-Type", "application/json");

                // Set timeout
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);

                // Vernon debug
                Log.d(LOG_TAG, urlConnection.getRequestMethod() + " " +
                               urlConnection.getURL().toString());

                // Send and get response
                // getResponseCode() will automatically trigger connect()
                int responseCode = urlConnection.getResponseCode();
                String responseMsg = urlConnection.getResponseMessage();
                Log.d(LOG_TAG, "Response " + responseCode + " " + responseMsg);
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                // Get items from body
                InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
                items = new Gson().fromJson(in, Item2[].class);
            } catch (JsonIOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Network may be unavailable while querying items");
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Get wrong JSON data while querying items");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Wrong query item URL " + QUERY_ITEM_URL);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Querying items failed");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Unhandled exception " + e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            if (items == null) {
                Log.d(LOG_TAG, "Got 0 item");
            } else {
                Log.d(LOG_TAG, "Got " + items.length + " items");
                // Vernon debug
                for (Item2 i : items) {
                    Log.d(LOG_TAG, i.toString());
                }
            }

            return items;
        }
    }

    /**
     * {@link android.widget.BaseAdapter} which displays items.
     */
    private class GridAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (items == null) {
                return 0;
            }
            return items.length;
        }

        @Override
        public Item2 getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.grid_item, viewGroup, false);
            }

            try {
                final Item2 item = getItem(position);

                // Load the thumbnail image
                ImageView image = (ImageView) view.findViewById(R.id.imageview_item);
                Picasso.with(image.getContext()).load(item.getThumbnail()).into(image);

                // Calculate distance
                int meters = -1;
                float results[] = new float[1];
                double dstLatitude = item.getLatitude();
                double dstLongitude = item.getLongitude();
                if (here != null && dstLatitude != 0 && dstLongitude != 0) {
                    Location.distanceBetween(here.getLatitude(), here.getLongitude(),
                                           dstLatitude, dstLongitude, results);
                    meters = (int) results[0];
                }

                // Set the TextView's contents
                TextView name = (TextView) view.findViewById(R.id.textview_name);
                if (meters == -1) {
                    name.setText(item.getAttendant() + "/" + item.getPeople());
                } else {
                    name.setText(item.getAttendant() + "/" + item.getPeople() + "    " + meters + "m");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return view;
        }
    }

    public GridView getmGridView() {
        return mGridView;
    }
}
