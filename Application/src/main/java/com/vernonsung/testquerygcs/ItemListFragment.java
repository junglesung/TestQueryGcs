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
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Fragment;
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

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.melnykov.fab.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ItemListFragment extends Fragment {
    /**
     * The methods that the activity which contains the fragment should implement
     */
    public interface ItemListFragmentListener {
        void onCreateItem();                   // To create a new item
        void onShowItemDetail(Item2 item);     // To show item detail
        String onGetInstanceId();              // Get instance ID from the activity
    }

    private static final String LOG_TAG = "TestGood";

    // Current location
    Location here;
    // Item list
    private Item2[] items;
    // Automatically retry when network is OK.
    boolean flagRefreshNeeded;

    // Threads
    private QueryItemTask mQueryItemTask;

    // Listener
    private ItemListFragmentListener itemListFragmentListener;

    // UI
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private GridView mGridView;
    private GridAdapter mAdapter;
    private FloatingActionButton fab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Notify the system to allow an options menu for this fragment.
//        setHasOptionsMenu(true);

        // Initial variables
        here = null;
        items = null;
        flagRefreshNeeded = true;
        mQueryItemTask = null;
        mAdapter = new GridAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);

        // Retrieve UI instances
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshItem);
        mGridView = (GridView) view.findViewById(R.id.gridViewItems);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);

        // Setup UI
        /**
         * Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
          */
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showDetail(adapterView, view, i, l);
            }
        });
        fab.attachToListView(mGridView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createItem();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ItemListFragmentListener) {
            itemListFragmentListener = (ItemListFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ItemListFragmentListener");
        }
    }

    /**
     * Deprecated in API level 23. Keep it here for backward compatibility
     */
    @Deprecated
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ItemListFragmentListener) {
            itemListFragmentListener = (ItemListFragmentListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement ItemListFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        itemListFragmentListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

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
                refresh();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when an item in the {@link android.widget.GridView} is clicked. Here will launch the
     * {@link ItemDetailFragment}, using the Scene Transition animation functionality.
     */
    public void showDetail(AdapterView<?> adapterView, View view, int position, long id) {
        // Get the specified item
        if (position > items.length) {
            Log.e(LOG_TAG, "User click position " + position + " is out of item number " + items.length);
            return;
        }
        if (itemListFragmentListener != null) {
            itemListFragmentListener.onShowItemDetail(items[position]);
        }
    }

    /**
     * Reload items from the APP server
     */
    public void refresh() {
        // Make sure it's not refreshing
        if (mQueryItemTask != null && mQueryItemTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Log.d(LOG_TAG, getString(R.string.no_network_connection_available));
            Toast.makeText(getActivity(), getString(R.string.no_network_connection_available), Toast.LENGTH_SHORT).show();
            return;
        }
        // Get Google Instance ID
        String instanceId = itemListFragmentListener.onGetInstanceId();
        if (instanceId == null || instanceId.isEmpty()) {
            Log.d(LOG_TAG, getString(R.string.app_is_not_registered_please_check_internet_and_retry_later));
            Toast.makeText(getActivity(), getString(R.string.app_is_not_registered_please_check_internet_and_retry_later), Toast.LENGTH_LONG).show();
            return;
        }
        // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
        mSwipeRefreshLayout.setRefreshing(true);
        // Execute querying thread
        mQueryItemTask = new QueryItemTask(instanceId);
        mQueryItemTask.execute();
    }

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

    // Get the latest items from the server in background
    private class QueryItemTask extends AsyncTask<Void, Void, Item2[]> {
        static final String QUERY_ITEM_URL = "https://aliza-1148.appspot.com/api/0.1/items";
        private String instanceId;
        // Screen orientation. Save and disable screen rotation in order to prevent screen rotation destroying the activity and the AsyncTask.
        private int screenOrientation;

        public QueryItemTask(String instanceId) {
            this.instanceId = instanceId;
        }

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
                urlConnection.setRequestProperty(MyConstants.HTTP_HEADER_INSTANCE_ID, instanceId);
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

    private void createItem() {
        // Call the activity to change fragment
        if (itemListFragmentListener != null) {
            itemListFragmentListener.onCreateItem();
        }
    }
}
