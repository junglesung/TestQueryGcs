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

import com.google.android.gms.iid.InstanceID;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
public class ItemDetailFragment extends Fragment
                             implements PhoneNumberDialogFragment.PhoneNumberDialogListener {

    // Results of making a HTTP request to the server
    public enum UpdateItemStatus {
        SUCCESS,
        ITEM_CLOSED,
        ANDROID_FAILURE,
        SERVER_FAILURE
    }

    private static final String LOG_TAG = "TestGood";

    // the fragment initialization parameters
    private static final String ARG_ID = "id";                // String

    // Extra name for the ID parameter
    public static final String EXTRA_PARAM_ID = "detail:_id";

    // View name of the header image. Used for activity scene transitions
    public static final String VIEW_NAME_HEADER_IMAGE = "detail:header:image";

    // View name of the header title. Used for activity scene transitions
    public static final String VIEW_NAME_HEADER_TITLE = "detail:header:title";

    // RCF 3339 time format from the server
    public static final String RFC3339FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    // UI
    private ImageView mHeaderImageView;
    private TextView mHeaderTitle;
    private ImageButton mButtonPlus;
    private ImageButton mButtonMinus;
    private TextView mTextViewIntroduction;
    private ImageButton mButtonCallPhone;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // Properties
    private Item2 mItem;
    private int myAttendant = 0;
    // User input phone number got from dialog
    private String mPhoneNumber;
    // Automatically retry when network is OK.
    boolean flagRefreshNeeded = false;

    // Threads
    private UpdateItemTask mUpdateItemTask;
    private GetItemTask mGetItemTask;

    public ItemDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param item The item to show detail
     * @return A new instance of fragment ItemDetailFragment.
     */
    public static ItemDetailFragment newInstance(Item2 item) {
        ItemDetailFragment fragment = new ItemDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, item.getId());
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_detail, container, false);
        // Retrieve the SwipeRefreshLayout and ListView instances
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh_detail);
        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);

        mHeaderImageView = (ImageView) view.findViewById(R.id.imageview_header);
        mHeaderTitle = (TextView) view.findViewById(R.id.textview_title);
        mButtonPlus = (ImageButton) view.findViewById(R.id.imageButton_plus);
        mButtonMinus = (ImageButton) view.findViewById(R.id.imageButton_minus);
        mTextViewIntroduction = (TextView) view.findViewById(R.id.textview_introduction);
        mButtonCallPhone = (ImageButton) view.findViewById(R.id.imageButton_callPhone);

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

        mButtonCallPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callOthersPhone();
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(LOG_TAG, "Start swipe refresh");
                initiateRefresh();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Show item data
        refreshByIntentItemId(savedInstanceState);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        PhoneNumberDialogFragment mPhoneNumberDialogFragment;
        try {
            mPhoneNumberDialogFragment = (PhoneNumberDialogFragment) dialog;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(dialog.toString()
                    + " must be PhoneNumberDialogFragment");
        }
        mPhoneNumber = mPhoneNumberDialogFragment.getPhoneNumber();
        if (mPhoneNumber == null || mPhoneNumber.isEmpty()) {
            Log.d(LOG_TAG, "Phone number is empty");
            Toast.makeText(getActivity(), getString(R.string.wrong_phone_number_format_please_enter_again), Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(LOG_TAG, "Got phone number " + mPhoneNumber);

        // Continue creating the item
        attend();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Log.d(LOG_TAG, "Return from Phone Dialog canceled");
    }

    // Called by onCreate()
    // Get item ID from intent and trigger refresh to get item data from the server and show
    private void refreshByIntentItemId(Bundle savedInstanceState) {
        // Get item ID from intent
        String id = getArguments().getString(ARG_ID);
        if (id == null) {
            throw new RuntimeException(getActivity().toString()
                    + " must give item ID as argument");
        }
        mItem = new Item2();
        mItem.setId(id);

        if (savedInstanceState != null) {
            initiateRefresh();
        } else {
            flagRefreshNeeded = true;
        }
    }

    // Show detail information
    private void showItem() {
        // Show attendants.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String userId = sharedPreferences.getString(MyConstants.USER_ID, "");
        if (userId.isEmpty()) {
            Log.d(LOG_TAG, "Got empty user ID. Go to main activity.");
            Toast.makeText(getActivity(), getString(R.string.data_is_out_of_date_reload_automatically), Toast.LENGTH_LONG).show();
            NavUtils.navigateUpFromSameTask(getActivity());
        }
        for (Item2.ItemMember member: mItem.getMembers()) {
            if (member.getUserkey().equals(userId)) {
                myAttendant = member.getAttendant();
                Log.d(LOG_TAG, "I attended " + myAttendant + " in item " + mItem.getId());
            }
        }
        mHeaderTitle.setText("(" + myAttendant + ") " + mItem.getAttendant() + "/" + mItem.getPeople());

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
            Log.d(LOG_TAG, "Wrong format CreateTime from server. Show it directly");
        }
        mTextViewIntroduction.setText(timeString);

        // Show the image
        loadFullSizeImage();

        // Show call button
        updatePhoneButtonVisibility();
    }

    /**
     * Load the item's thumbnail image into our {@link ImageView}.
     */
    private void loadThumbnail() {
        Picasso.with(mHeaderImageView.getContext())
                .load(mItem.getThumbnail())
                .into(mHeaderImageView);
    }

    /**
     * Load the item's full-size image into our {@link ImageView}.
     */
    private void loadFullSizeImage() {
        Picasso.with(mHeaderImageView.getContext())
                .load(mItem.getImage())
                .noFade()
                .noPlaceholder()
                .into(mHeaderImageView);
    }

    // Set call buttons' visibility
    private void updatePhoneButtonVisibility() {
        if (myAttendant > 0 && mItem.getMembers().length >= 2) {
            // Show call button
            mButtonCallPhone.setVisibility(View.VISIBLE);
        } else {
            mButtonCallPhone.setVisibility(View.INVISIBLE);
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

    // Show dialog for users to input/confirm their phone numbers
    // Dialog will really shows after returning to activity
    public void showPhoneNumberDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new PhoneNumberDialogFragment();
        dialog.show(getFragmentManager(), "PhoneNumberDialogFragment");
    }

    // Attend in the item
    private void attend() {
        // Check phone number
        if (myAttendant == 0 && (mPhoneNumber == null || mPhoneNumber.isEmpty())) {
            showPhoneNumberDialog();
            // Dialog will really shows after returning to activity
            return;
        }

        modifyAttendant(1);
    }

    // Leave the item
    private void leave() {
        modifyAttendant(-1);
    }

    private void modifyAttendant(int change) {
        // Check Google Instance ID registration
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isTokenSentToServer = sharedPreferences.getBoolean(MyConstants.SENT_TOKEN_TO_SERVER, false);
        if (!isTokenSentToServer) {
            Toast.makeText(getActivity(), getString(R.string.app_is_not_registered_please_check_internet_and_retry_later), Toast.LENGTH_LONG).show();
            return;
        }

        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getActivity(), getString(R.string.no_network_connection_available), Toast.LENGTH_SHORT).show();
            return;
        }
        if (mUpdateItemTask != null && mUpdateItemTask .getStatus() == AsyncTask.Status.RUNNING) {
            Toast.makeText(getActivity(), getString(R.string.server_is_busy_please_try_again_later), Toast.LENGTH_SHORT).show();
            return;
        }
        // Execute querying thread
        mUpdateItemTask = new UpdateItemTask();
        mUpdateItemTask.execute(change);
    }

    private void navigateUp() {
        getActivity().getFragmentManager().popBackStack();
    }

    private void showItemCloseDialog() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.go_to_the_item_list)
                .setTitle(R.string.item_was_closed_since_the_owner_left);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                navigateUp();
            }
        });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        // 4. Show the dialog
        dialog.show();
    }

    // Update the item on the server in background
    private class UpdateItemTask extends AsyncTask<Integer, Void, Void> {
        static final String UPDATE_ITEM_URL = "https://aliza-1148.appspot.com/api/0.1/items";
        private int change;
        private UpdateItemStatus status = UpdateItemStatus.SUCCESS;
        // Screen orientation. Save and disable screen rotation in order to prevent screen rotation destroying the activity and the AsyncTask.
        private int screenOrientation;

        @Override
        protected void onPreExecute() {
            // Disable screen rotation
            screenOrientation = getActivity().getRequestedOrientation();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        @Override
        protected Void doInBackground(Integer... params) {
            // Get the change number
            if (params.length < 1) {
                Log.e(LOG_TAG, "No specified number");
                status = UpdateItemStatus.ANDROID_FAILURE;
                return null;
            }
            change = params[0];
            updateItem();
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            switch (status) {
                case SUCCESS:
                    // Tell all activities to refresh
                    sendRefreshBroadcast();
                    break;
                case ITEM_CLOSED:
                    showItemCloseDialog();
                    break;
                case ANDROID_FAILURE:
                    Toast.makeText(getActivity(), getString(R.string.please_check_the_network_and_try_again), Toast.LENGTH_SHORT).show();
                    break;
                case SERVER_FAILURE:
                    Toast.makeText(getActivity(), getString(R.string.server_is_busy_please_try_again_later), Toast.LENGTH_SHORT).show();
                    break;
            }

            // Enable screen rotation
            getActivity().setRequestedOrientation(screenOrientation);
        }

        // HTTP PUT change to the server
        // Return 0 on success
        // Return 1 on Android failure
        // Return 2 on server failure
        private void updateItem() {
            URL url;
            HttpsURLConnection urlConnection = null;
            int size;
            byte[] data;
            OutputStream out;
            String itemUrl = UPDATE_ITEM_URL + "/" + mItem.getId();

            try {
                url = new URL(itemUrl);
                urlConnection = (HttpsURLConnection) url.openConnection();

                // Set authentication instance ID
                urlConnection.setRequestProperty(MyConstants.HTTP_HEADER_INSTANCE_ID, InstanceID.getInstance(getActivity()).getId());
                // Set content type
                urlConnection.setRequestProperty("Content-Type", "application/json");

                // To upload data to a web server, configure the connection for output using setDoOutput(true). It will use POST if setDoOutput(true) has been called.
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("PUT");

                // Convert item to JSON string
                JSONObject jsonItem = new JSONObject();
                JSONObject jsonMember = new JSONObject();
                JSONArray  jsonMembers = new JSONArray();
                jsonMember.put("attendant", change);
                if (change >= 1 && mPhoneNumber != null && !mPhoneNumber.isEmpty()) {
                    jsonMember.put("phonenumber", mPhoneNumber);
                }
                jsonMembers.put(jsonMember);
                jsonItem.put("members", jsonMembers);
                data = jsonItem.toString().getBytes();

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
                    status = UpdateItemStatus.ANDROID_FAILURE;
                    return;
                }

                // Set timeout
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);

                // Vernon debug
                Log.d(LOG_TAG, urlConnection.getRequestMethod() + " " +
                        urlConnection.getURL().toString() + new String(data));

                // Send and get response
                // getResponseCode() will automatically trigger connect()
                int responseCode = urlConnection.getResponseCode();
                String responseMsg = urlConnection.getResponseMessage();
                Log.d(LOG_TAG, "Response " + responseCode + " " + responseMsg);
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.d(LOG_TAG, "Server says the item was closed");
                    status = UpdateItemStatus.ITEM_CLOSED;
                    return;
                }
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.d(LOG_TAG, "Update item attendant " + change + " failed");
                    status = UpdateItemStatus.SERVER_FAILURE;
                    return;
                }

                // Vernon debug
                Log.d(LOG_TAG, "Update item attendant " + change + " successfully");

            } catch (Exception e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "Update item failed because " + e.getMessage());
                status = UpdateItemStatus.ANDROID_FAILURE;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
    }

    private void sendRefreshBroadcast() {
        // Notify UI to refresh
        Intent refreshIntent = new Intent(MyConstants.REFRESH);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(refreshIntent);
    }

    // Make a phone call to the owner or the member
    private void callOthersPhone() {
        String phoneNumber;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String userId = sharedPreferences.getString(MyConstants.USER_ID, "");
        if (userId.equals("")) {
            Log.e(LOG_TAG, "User ID is empty. Maybe I'm not registered. Restart the APP.");
            Toast.makeText(getActivity(), R.string.app_is_not_registered_please_check_internet_and_retry_later, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mItem.getMembers().length < 2) {
            Log.w(LOG_TAG, "Item member " + mItem.getMembers().length + " < 2 means it's out of date. Please refresh.");
            Toast.makeText(getActivity(), R.string.data_is_out_of_date_please_refresh, Toast.LENGTH_LONG).show();
            return;
        }
        if (mItem.getMembers()[0].getUserkey().equals(userId)) {
            // Call the member
            phoneNumber = mItem.getMembers()[1].getPhonenumber();
        } else {
            // Call the owner
            phoneNumber = mItem.getMembers()[0].getPhonenumber();
        }

        // Open Phone APP
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
            Log.d(LOG_TAG, "Can't find phone APP");
            Toast.makeText(getActivity(), R.string.phone_app_is_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getActivity(), R.string.call_your_partner, Toast.LENGTH_LONG).show();
        startActivity(intent);
    }

    // Refresh the item information from the server
    private void initiateRefresh() {
        // Check Google Instance ID registration
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isTokenSentToServer = sharedPreferences.getBoolean(MyConstants.SENT_TOKEN_TO_SERVER, false);
        if (!isTokenSentToServer) {
            Toast.makeText(getActivity(), getString(R.string.app_is_not_registered_please_check_internet_and_retry_later), Toast.LENGTH_LONG).show();
            return;
        }

        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            mSwipeRefreshLayout.setRefreshing(true);
            // Execute querying thread
            mGetItemTask = new GetItemTask();
            mGetItemTask.execute();
        } else {
            Toast.makeText(getActivity(), getString(R.string.no_network_connection_available), Toast.LENGTH_SHORT).show();
        }
    }

    // Get the latest item from the server in background
    private class GetItemTask extends AsyncTask<Void, Void, Item2> {
        static final String GET_ITEM_URL = "https://aliza-1148.appspot.com/api/0.1/items";
        private UpdateItemStatus status = UpdateItemStatus.SUCCESS;
        // Screen orientation. Save and disable screen rotation in order to prevent screen rotation destroying the activity and the AsyncTask.
        private int screenOrientation;

        @Override
        protected void onPreExecute() {
            // Disable screen rotation
            screenOrientation = getActivity().getRequestedOrientation();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        @Override
        protected Item2 doInBackground(Void... params) {
            return getItem();
        }

        @Override
        protected void onPostExecute(Item2 v) {
            switch (status) {
                case SUCCESS:
                    // Store item info
                    mItem = v;
                    // Show new item info
                    showItem();
                    break;
                case ITEM_CLOSED:
                    showItemCloseDialog();
                    break;
                case ANDROID_FAILURE:
                    Toast.makeText(getActivity(), getString(R.string.please_check_the_network_and_try_again), Toast.LENGTH_SHORT).show();
                    break;
                case SERVER_FAILURE:
                    Toast.makeText(getActivity(), getString(R.string.server_is_busy_please_try_again_later), Toast.LENGTH_SHORT).show();
                    break;
            }
            // Stop the refreshing indicator
            mSwipeRefreshLayout.setRefreshing(false);

            // Enable screen rotation
            getActivity().setRequestedOrientation(screenOrientation);
        }

        // HTTP GET item from the server
        // Return 0 on success
        private Item2 getItem() {
            URL url;
            HttpsURLConnection urlConnection = null;
            Item2 item = null;
            String itemUrl = GET_ITEM_URL + "/" + mItem.getId();

            try {
                url = new URL(itemUrl);
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
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.d(LOG_TAG, "Server says the item was closed");
                    status = UpdateItemStatus.ITEM_CLOSED;
                    return null;
                }
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.d(LOG_TAG, "Get item " + mItem.getId() + " failed");
                    status = UpdateItemStatus.SERVER_FAILURE;
                    return null;
                }

                // Get items from body
                InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
                item = new Gson().fromJson(in, Item2.class);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "Get item failed because " + e.getMessage());
                status = UpdateItemStatus.ANDROID_FAILURE;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return item;
        }
    }
}
