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

package com.example.android.activityscenetransitionbasic;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewCompat;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

/**
 * Our secondary Activity which is launched from {@link MainActivity}. Has a simple detail UI
 * which has a large banner image, title and body text.
 */
public class DetailActivity extends Activity {

    private static final String LOG_TAG = "Test";

    // Extra name for the ID parameter
    public static final String EXTRA_PARAM_ID = "detail:_id";
    public static final String EXTRA_PARAM_ITEM = "detail:_item";

    // View name of the header image. Used for activity scene transitions
    public static final String VIEW_NAME_HEADER_IMAGE = "detail:header:image";

    // View name of the header title. Used for activity scene transitions
    public static final String VIEW_NAME_HEADER_TITLE = "detail:header:title";

    // RCF 3339 time format from the server
    public static final String RFC3339FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    private ImageView mHeaderImageView;
    private TextView mHeaderTitle;
    private ImageButton mButtonPlus;
    private ImageButton mButtonMinus;
    private TextView mTextViewIntroduction;

    private Item2 mItem;

    // Threads
    private UpdateItemTask mUpdateItemTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details);

        // Get item in JSON format
        String itemJson = getIntent().getStringExtra(EXTRA_PARAM_ITEM);
        if (itemJson == null) {
            Log.e(LOG_TAG, "Get null item from intent");
            NavUtils.navigateUpFromSameTask(this);
        }

        // Transform JSON to item
        try {
            mItem = new Gson().fromJson(itemJson, Item2.class);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Wrong JSON format in transforming item");
            NavUtils.navigateUpFromSameTask(this);
        }

        mHeaderImageView = (ImageView) findViewById(R.id.imageview_header);
        mHeaderTitle = (TextView) findViewById(R.id.textview_title);
        mButtonPlus = (ImageButton) findViewById(R.id.imageButton_plus);
        mButtonMinus = (ImageButton) findViewById(R.id.imageButton_minus);
        mTextViewIntroduction = (TextView) findViewById(R.id.textview_introduction);

        mButtonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attend();
            }
        });

        mButtonMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leave();
            }
        });

        // BEGIN_INCLUDE(detail_set_view_name)
        /**
         * Set the name of the view's which will be transition to, using the static values above.
         * This could be done in the layout XML, but exposing it via static variables allows easy
         * querying from other Activities
         */
        ViewCompat.setTransitionName(mHeaderImageView, VIEW_NAME_HEADER_IMAGE);
        ViewCompat.setTransitionName(mHeaderTitle, VIEW_NAME_HEADER_TITLE);
        // END_INCLUDE(detail_set_view_name)

        loadItem();
    }

    private void loadItem() {
        // Show detail information
        mHeaderTitle.setText(mItem.getAttendant() + "/" + mItem.getPeople());

        // Transform time format and show
        SimpleDateFormat sdf1 = new SimpleDateFormat(RFC3339FORMAT, Locale.US);  // in format
        DateFormat sdf2 = DateFormat.getDateTimeInstance();  // out format
        String timeString = mItem.getCreatetime();
        sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));  // Get UTC time from server
        try {
            Date d = sdf1.parse(timeString);
            timeString = sdf2.format(d);
        } catch (ParseException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "Wrong format CreateTime from server. Show it directory");
        }
        mTextViewIntroduction.setText(timeString);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && addTransitionListener()) {
            // If we're running on Lollipop and we have added a listener to the shared element
            // transition, load the thumbnail. The listener will load the full-size image when
            // the transition is complete.
            loadThumbnail();
        } else {
            // If all other cases we should just load the full-size image now
            loadFullSizeImage();
        }
    }

    /**
     * Load the item's thumbnail image into our {@link ImageView}.
     */
    private void loadThumbnail() {
        Picasso.with(mHeaderImageView.getContext())
                .load(mItem.getThumbnailUrl())
                .noFade()
                .into(mHeaderImageView);
    }

    /**
     * Load the item's full-size image into our {@link ImageView}.
     */
    private void loadFullSizeImage() {
        Picasso.with(mHeaderImageView.getContext())
                .load(mItem.getPhotoUrl())
                .noFade()
                .noPlaceholder()
                .into(mHeaderImageView);
    }

    /**
     * Try and add a {@link Transition.TransitionListener} to the entering shared element
     * {@link Transition}. We do this so that we can load the full-size image after the transition
     * has completed.
     *
     * @return true if we were successful in adding a listener to the enter transition
     */
    private boolean addTransitionListener() {
        final Transition transition = getWindow().getSharedElementEnterTransition();

        if (transition != null) {
            // There is an entering shared element transition so add a listener to it
            transition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    // As the transition has ended, we can now load the full-size image
                    loadFullSizeImage();

                    // Make sure we remove ourselves as a listener
                    transition.removeListener(this);
                }

                @Override
                public void onTransitionStart(Transition transition) {
                    // No-op
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    // Make sure we remove ourselves as a listener
                    transition.removeListener(this);
                }

                @Override
                public void onTransitionPause(Transition transition) {
                    // No-op
                }

                @Override
                public void onTransitionResume(Transition transition) {
                    // No-op
                }
            });
            return true;
        }

        // If we reach here then we have not added a listener
        return false;
    }

    // Attend in the item
    private void attend() {
        modifyAttendant(1);
    }

    // Leave the item
    private void leave() {
        modifyAttendant(-1);
    }

    private void modifyAttendant(int change) {
        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // Execute querying thread
            mUpdateItemTask = new UpdateItemTask();
            mUpdateItemTask.execute(change);
        } else {
            Toast.makeText(this, getString(R.string.no_network_connection_available), Toast.LENGTH_SHORT).show();
        }
    }

    // Get the latest items from the server in background
    private class UpdateItemTask extends AsyncTask<Integer, Void, Integer> {
        static final String UPDATE_ITEM_URL = "https://testgcsserver.appspot.com/api/0.1/items";
        private int change;

        @Override
        protected Integer doInBackground(Integer... params) {
            // Get the change number
            if (params.length < 1) {
                Log.e(LOG_TAG, "No specified number");
                return 0;
            }
            change = params[0];
            if (updateItem() == 0) {
                return change;
            } else {
                return 0;
            }
        }

        @Override
        protected void onPostExecute(Integer _change) {
            super.onPostExecute(_change);
            // Update memory
            mItem.setAttendant(mItem.getAttendant() + _change);
            // Update UI
            mHeaderTitle.setText(mItem.getAttendant() + "/" + mItem.getPeople());
        }

        // HTTP PUT change to the server
        // Return 0 on success
        // Return -1 on failure
        private int updateItem() {
            URL url;
            HttpsURLConnection urlConnection = null;
            int size;
            byte[] data;
            OutputStream out;
            String itemUrl = UPDATE_ITEM_URL + "/" + mItem.getId();

            try {
                url = new URL(itemUrl);
                urlConnection = (HttpsURLConnection) url.openConnection();

                // Set content type
                urlConnection.setRequestProperty("Content-Type", "application/json");

                // To upload data to a web server, configure the connection for output using setDoOutput(true). It will use POST if setDoOutput(true) has been called.
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("PUT");

                // Convert item to JSON string
                data = new JSONObject().put("attendant", change).toString().getBytes();

                // For best performance, you should call either setFixedLengthStreamingMode(int) when the body length is known in advance, or setChunkedStreamingMode(int) when it is not. Otherwise HttpURLConnection will be forced to buffer the complete request body in memory before it is transmitted, wasting (and possibly exhausting) heap and increasing latency.
                size = data.length;
                if (size > 0) {
                    urlConnection.setFixedLengthStreamingMode(size);
                } else {
                    // Set default chunk size
                    urlConnection.setChunkedStreamingMode(0);
                }

                // Get the OutputStream of HTTP client
                out = new BufferedOutputStream(urlConnection.getOutputStream());
                // Copy from file to the HTTP client
                out.write(data);
                // Make sure to close streams, otherwise "unexpected end of stream" error will happen
                out.close();

                // Check canceled
                if (isCancelled()) {
                    Log.d(LOG_TAG, "Updating item canceled");
                    return -1;
                }

                // Set timeout
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);

                // Send and get response
                // getResponseCode() will automatically trigger connect()
                int responseCode = urlConnection.getResponseCode();
                String responseMsg = urlConnection.getResponseMessage();
                Log.d(LOG_TAG, "Response " + responseCode + " " + responseMsg);
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.d(LOG_TAG, "Update item attendant " + change + " failed");
                    return -1;
                }

                // Vernon debug
                Log.d(LOG_TAG, "Update item attendant " + change + " successfully");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return 0;
        }
    }

}
