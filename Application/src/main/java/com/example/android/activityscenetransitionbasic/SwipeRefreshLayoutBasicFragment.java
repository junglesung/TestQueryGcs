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

package com.example.android.activityscenetransitionbasic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

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

    private static final String LOG_TAG = "Test";

    private Item2[] items;

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
//        setHasOptionsMenu(true);

        // Initial variables
        items = null;
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

        // Get the latest items from server
        initiateRefresh();
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
        }

        return super.onOptionsItemSelected(item);
    }
    // END_INCLUDE (setup_refresh_menu_listener)

    /**
     * Called when an item in the {@link android.widget.GridView} is clicked. Here will launch the
     * {@link DetailActivity}, using the Scene Transition animation functionality.
     */
    public void showDetail(AdapterView<?> adapterView, View view, int position, long id) {
        Item item = (Item) adapterView.getItemAtPosition(position);

        // Construct an Intent as normal
        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_PARAM_ID, item.getId());

        // BEGIN_INCLUDE(start_activity)
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

    // BEGIN_INCLUDE (initiate_refresh)
    /**
     * By abstracting the refresh process to a single method, the app allows both the
     * SwipeGestureLayout onRefresh() method and the Refresh action item to refresh the content.
     */
    private void initiateRefresh() {
        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
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

    // Get the latest items from the server in background
    private class QueryItemTask extends AsyncTask<Void, Void, Item2[]> {

        static final String QUERY_ITEM_URL = "https://testgcsserver.appspot.com/api/0.1/items";

        @Override
        protected Item2[] doInBackground(Void... params) {
            return getItems();
        }

        @Override
        protected void onPostExecute(Item2[] result) {
            super.onPostExecute(result);

            // Tell the Fragment that the refresh has completed
            onRefreshComplete(result);
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

                // Set content type
                urlConnection.setRequestProperty("Content-Type", "application/json");

                // Set timeout
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);

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
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            if (items == null) {
                Log.d(LOG_TAG, "Querying items failed");
            } else {
                Log.d(LOG_TAG, "Got " + items.length + "items");
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

            final Item2 item = getItem(position);

            // Load the thumbnail image
            ImageView image = (ImageView) view.findViewById(R.id.imageview_item);
            Picasso.with(image.getContext()).load(item.getThumbnailUrl()).into(image);

            // Set the TextView's contents
            TextView name = (TextView) view.findViewById(R.id.textview_name);
            name.setText(item.getAttendant() + "/" + item.getPeople());

            return view;
        }
    }

    public GridView getmGridView() {
        return mGridView;
    }
}
