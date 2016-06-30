package com.vernonsung.testquerygcs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.iid.InstanceID;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class CreateItemFragment extends Fragment
                             implements PhoneNumberDialogFragment.PhoneNumberDialogListener {
    /**
     * To fetch latest location
     */
    public interface OnFetchLocationListener {
        Location onFetchLocation();
    }

    enum CreateItemState {
        INITIAL, UPLOADING_IMAGE, READY_TO_SEND, SENDING, FINISHED
    }

    private static final String LOG_TAG = "TestGood";

    // The request code to invoke a camera APP
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    // The request code to choose an image
    private static final int REQUEST_IMAGE_BROWSE = 2;

    // URL
    private final String UPLOAD_IMAGE_URL = "https://aliza-1148.appspot.com/api/0.1/storeImage";
    private final String CREATE_ITEM_URL = "https://aliza-1148.appspot.com/api/0.1/items";
    // Take photo folder
    private File mStorageDir;
    // Current image path
    private Uri mCurrentPhotoUri;
    private String mGcsPhotoUrl;
    private String mGcsThumbnailUrl;
    // User input phone number got from dialog
    private String mPhoneNumber;

    // State
    private CreateItemState mState;

    // Threads
    private UploadImageTask mUploadImageTask;
    private CreateItemTask mCreateItemTask;

    // Listener
    private OnFetchLocationListener onFetchLocationListener;

    // UI
    private ImageView mImageView;
    private ImageButton mButtonCamera;
    private ImageButton mButtonGallery;
    private Button mButtonMore;
    private Button mButtonSend;
    private TextView mTextViewInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_item, container, false);

        // UI
        mImageView = (ImageView) view.findViewById(R.id.imageview_create_item);

        mButtonCamera = (ImageButton) view.findViewById(R.id.imagebutton_camera);
        mButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        mButtonGallery = (ImageButton) view.findViewById(R.id.imagebutton_gallery);
        mButtonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseAnImage();
            }
        });

        mButtonMore = (Button) view.findViewById(R.id.button_create_item_more);
        mButtonMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchLocation();
            }
        });

        mButtonSend = (Button) view.findViewById(R.id.button_create_item_send);
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOrCancel();
            }
        });

        mTextViewInfo = (TextView) view.findViewById(R.id.textview_information);

        // Restore or initial
        mStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));
        if (savedInstanceState != null) {
            mCurrentPhotoUri = savedInstanceState.getParcelable(MyConstants.CREATEITEMACTIVITY_MCURRENTPHOTOURI);
            if (mCurrentPhotoUri != null) {
                Picasso.with(getActivity()).load(mCurrentPhotoUri).into(mImageView);
            }
            mGcsPhotoUrl = savedInstanceState.getString(MyConstants.CREATEITEMACTIVITY_MGCSPHOTOURL);
            mPhoneNumber = savedInstanceState.getString(MyConstants.CREATEITEMACTIVITY_MPHONENUMBER);
            CreateItemState state = (CreateItemState) savedInstanceState.getSerializable(MyConstants.CREATEITEMACTIVITY_MSTATE);
            if (state != null) {
                changeState(state);
            } else {
                changeState(CreateItemState.INITIAL);
            }
        } else {
            changeState(CreateItemState.INITIAL);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFetchLocationListener) {
            onFetchLocationListener = (OnFetchLocationListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFetchLocationListener");
        }
    }

    /**
     * Deprecated in API level 23. Keep it here for backward compatibility
     */
    @Deprecated
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFetchLocationListener) {
            onFetchLocationListener = (OnFetchLocationListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFetchLocationListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onFetchLocationListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(MyConstants.CREATEITEMACTIVITY_MCURRENTPHOTOURI, mCurrentPhotoUri);
        outState.putString(MyConstants.CREATEITEMACTIVITY_MGCSPHOTOURL, mGcsPhotoUrl);
        outState.putString(MyConstants.CREATEITEMACTIVITY_MPHONENUMBER, mPhoneNumber);
        outState.putSerializable(MyConstants.CREATEITEMACTIVITY_MSTATE, mState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // The ACTION_GET_CONTENT intent was sent with the request code
        // START_ACTIVITY_ID_CHOOSE_AN_IMAGE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                onActivityResultImageCapture(resultCode, data);
                break;
            case REQUEST_IMAGE_BROWSE:
                onActivityResultImageBrowse(resultCode, data);
                break;
        }
    }

    private void onActivityResultImageCapture(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // Display image
            Picasso.with(getActivity()).load(mCurrentPhotoUri).into(mImageView);
            galleryAddPic();
            uploadImage();
        }
    }

    private void onActivityResultImageBrowse(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (data != null) {
                mCurrentPhotoUri = data.getData();
                Log.i(LOG_TAG, "Chosen image URI " + mCurrentPhotoUri.toString());
                Picasso.with(getActivity()).load(mCurrentPhotoUri).into(mImageView);
                uploadImage();
            }
        }
    }

    private void uploadImage() {
        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // Change state
            changeState(CreateItemState.UPLOADING_IMAGE);
            // Execute uploading thread
            mUploadImageTask = new UploadImageTask();
            mUploadImageTask.execute(mCurrentPhotoUri);
        } else {
            Toast.makeText(getActivity(), getString(R.string.no_network_connection_available), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Implementation of AsyncTask, to fetch the data in the background away from
     * the UI thread.
     */
    private class UploadImageTask extends AsyncTask<Uri, Integer, String> {
        // Screen orientation. Save and disable screen rotation in order to prevent screen rotation destroying the activity and the AsyncTask.
        private int screenOrientation;
        String imageUrl = null;
        String thumbnailUrl = null;

        @Override
        protected void onPreExecute() {
            // Disable screen rotation
            screenOrientation = getActivity().getRequestedOrientation();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        // Return the URL in Google Cloud Storage
        // Return null when failed
        @Override
        protected String doInBackground(Uri... uris) {
            // Deal with one image at a time
            if (uris.length != 1) {
                Log.e(LOG_TAG, "No specified URI");
                return null;
            }
            try {
                // Upload
                String url = sendAnImage(uris[0]);
                if (url == null) {
                    Log.e(LOG_TAG, "Upload the image failed");
                    return null;
                }
                return url;
            } catch (IOException e) {
                Log.e(LOG_TAG, getString(R.string.connection_error));
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            // Check parameter
            if (values.length == 0) {
                Log.e(LOG_TAG, "Upload thread publish no progress");
                return;
            }

            // Set text view
            int v = values[0];
            if (v >= 0) {
                String s = String.format(getString(R.string.uploading_image), v);
                mTextViewInfo.setText(s);
            }
        }

        /**
         * Uses the logging framework to display the output of the fetch
         * operation in the log fragment.
         */
        @Override
        protected void onPostExecute(String url) {
            mGcsPhotoUrl = url;
            mGcsThumbnailUrl = thumbnailUrl;
            if (url == null) {
                Toast.makeText(getActivity(), R.string.server_error_please_choose_a_jpeg_photo_and_try_again, Toast.LENGTH_LONG).show();
                changeState(CreateItemState.INITIAL);
            } else {
                // Upload image successfully
                Log.d(LOG_TAG, "Finish uploading an image");
                changeState(CreateItemState.READY_TO_SEND);
            }

            // Enable screen rotation
            getActivity().setRequestedOrientation(screenOrientation);
        }

        // Send an image to Google Cloud Storage
        // Return the URL of the uploaded image
        // Return null if failed
        private String sendAnImage(Uri uri) throws IOException {
            URL url = new URL(UPLOAD_IMAGE_URL);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            int size;
            OutputStream out;
            InputStream in = null;

            // Set authentication instance ID
            urlConnection.setRequestProperty(MyConstants.HTTP_HEADER_INSTANCE_ID, InstanceID.getInstance(getActivity()).getId());
            // Set content type
            urlConnection.setRequestProperty("Content-Type", "image/jpeg");

            try {
                // To upload data to a web server, configure the connection for output using setDoOutput(true). It will use POST if setDoOutput(true) has been called.
                urlConnection.setDoOutput(true);

                // For best performance, you should call either setFixedLengthStreamingMode(int) when the body length is known in advance, or setChunkedStreamingMode(int) when it is not. Otherwise HttpURLConnection will be forced to buffer the complete request body in memory before it is transmitted, wasting (and possibly exhausting) heap and increasing latency.
                size = fetchUriSize(uri);
                if (size > 0) {
                    urlConnection.setFixedLengthStreamingMode(size);
                } else {
                    // Set default chunk size
                    urlConnection.setChunkedStreamingMode(0);
                }

                // Get the OutputStream of HTTP client
                out = new BufferedOutputStream(urlConnection.getOutputStream());
                // Get the InputStream of the file
                InputStream inputstream = getActivity().getContentResolver().openInputStream(uri);
                if (inputstream == null) {
                    Log.d(LOG_TAG, "Can't open image file");
                    return null;
                }
                in = new BufferedInputStream(inputstream);
                // Copy from file to the HTTP client
                int byte_;
                int bytes = 0;
                int percent = 0;
                int percentNext = 0;
                while ((byte_ = in.read()) != -1) {
                    out.write(byte_);
                    // Count and update uploading progress
                    bytes += 100;
                    percentNext = bytes / size;
                    if (percentNext > percent) {
                        publishProgress(percentNext);  // 0~100%
                        percent = percentNext;
                    }
                    if (isCancelled()) {
                        Log.d(LOG_TAG, "Uploading image canceled");
                        break;
                    }
                }
                // Make sure to close streams, otherwise "unexpected end of stream" error will happen
                out.close();
                in.close();
                in = null;

                // Check canceled
                if (isCancelled()) {
                    Log.d(LOG_TAG, "Uploading image canceled");
                    return null;
                }

                // Set timeout
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);

                // Send and get response
                // getResponseCode() will automatically trigger connect()
                int responseCode = urlConnection.getResponseCode();
                String responseMsg = urlConnection.getResponseMessage();
                Log.d(LOG_TAG, "Response " + responseCode + " " + responseMsg);
                if (responseCode != HttpURLConnection.HTTP_CREATED) {
                    return null;
                }

                // Get image URL
                imageUrl = urlConnection.getHeaderField("Location");
                thumbnailUrl = urlConnection.getHeaderField("X-Thumbnail");
                Log.d(LOG_TAG, "Image URL " + imageUrl);
                Log.d(LOG_TAG, "Thumbnail URL " + thumbnailUrl);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
                if (in != null)
                    in.close();
            }
            return imageUrl;
        }
    }

    // Get file size to set HTTP POST body length
    private int fetchUriSize(Uri uri) {
        int size = -1;

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

        try {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getInt(sizeIndex);
                }
                Log.i(LOG_TAG, "Image size " + size + " byte(s)");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return size;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) == null) {
            Log.e(LOG_TAG, getString(R.string.no_camera_app));
            Toast.makeText(getActivity(), getString(R.string.no_camera_app), Toast.LENGTH_SHORT).show();
            return;
        }
        // Ensure external storage is writable
        if (!isExternalStorageWritable()) {
            Log.e(LOG_TAG, getString(R.string.external_storage_is_not_writable));
            Toast.makeText(getActivity(), getString(R.string.external_storage_is_not_writable), Toast.LENGTH_SHORT).show();
            return;
        }
        // Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            ex.printStackTrace();
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Create an image file name
    private File createImageFile() throws IOException {
        // File name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // Folder
        if (!mStorageDir.isDirectory() && !mStorageDir.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
            return null;
        }

        File image = new File(mStorageDir, imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoUri = Uri.fromFile(image);

        // Vernon debug
        Log.d(LOG_TAG, "Image " + mCurrentPhotoUri.toString());

        return image;
    }

    private void deleteMyPhoto() {
        if (!mStorageDir.isDirectory()) {
            // Folder is not created
            return;
        }
        String[] children = mStorageDir.list();
        for (String s: children) {
            File f = new File(mStorageDir, s);
            f.delete();
            try {
                getActivity().getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media.DATA
                                + "='"
                                + f.getPath()
                                + "'", null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(mCurrentPhotoUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    private void chooseAnImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("image/*");

        //第一個問題是Android系統找不到符合指定MIME類型的內容選取器，程式將會因為找不到可執行的Activity而直接閃退，這個問題甚至可能會沒辦法直接用try-catch來解決。第二個可能會遇到的問題是，當Android系統找到兩種以上可用的App或是Activity支援指定的MIME類型時，可能會自動使用其中一種，此時也許就會選到無法正常使用的App或是Activity，連帶使我們的App永遠無法正常使用。
        //要解決第一個找不到Activity的問題，可以事先使用PackageManager查詢可以使用該MIME類型的Activity列表來解決。而要解決第二個可用App或是Activity有兩個以上的問題的話，可以使用系統內建的Intent Chooser，跳出選單讓使用者選擇要使用哪個。
        PackageManager packageManager = getActivity().getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            // 如果有可用的Activity
            // 使用Intent Chooser
            Intent destIntent = Intent.createChooser(intent, "選取圖片");
            startActivityForResult(destIntent, REQUEST_IMAGE_BROWSE);
        } else {
            // 沒有可用的Activity
            Toast.makeText(getActivity(), getString(R.string.no_app_to_choose_an_image), Toast.LENGTH_SHORT).show();
        }
    }

    // Show dialog for users to input/confirm their phone numbers
    // Dialog will really shows after returning to activity
    public void showPhoneNumberDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new PhoneNumberDialogFragment();
        dialog.show(getFragmentManager(), "PhoneNumberDialogFragment");
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
        createItem();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Log.d(LOG_TAG, "Return from Phone Dialog canceled");
    }

    private Location fetchLocation() {
        if (onFetchLocationListener != null) {
            return onFetchLocationListener.onFetchLocation();
        }
        return null;
    }

    private void createItem() {
        // Check Google Instance ID registration
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isTokenSentToServer = sharedPreferences.getBoolean(MyConstants.SENT_TOKEN_TO_SERVER, false);
        if (!isTokenSentToServer) {
            Toast.makeText(getActivity(), getString(R.string.app_is_not_registered_please_check_internet_and_retry_later), Toast.LENGTH_LONG).show();
            return;
        }

        // Check phone number
        if (mPhoneNumber == null || mPhoneNumber.isEmpty()) {
            showPhoneNumberDialog();
            // Dialog will really shows after returning to activity
            return;
        }

        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getActivity(), getString(R.string.no_network_connection_available), Toast.LENGTH_LONG).show();
            return;
        }

        // Prepare event data
        Location location = fetchLocation();
        if (location == null) {
            return;
        }
        // Item2.CreateTime is determined by the server. So just set an empty string.
        Item2 item = new Item2();
        item.setImage(mGcsPhotoUrl);
        item.setThumbnail(mGcsThumbnailUrl);
        item.setPeople(2);
        item.setAttendant(1);
        item.setLatitude(location.getLatitude());
        item.setLongitude(location.getLongitude());
        Item2.ItemMember[] owner = new Item2.ItemMember[1];
        owner[0] = item.new ItemMember();
        owner[0].setAttendant(1);
        owner[0].setPhonenumber(mPhoneNumber);
        item.setMembers(owner);

        // Change state
        changeState(CreateItemState.SENDING);
        // Execute uploading thread
        mCreateItemTask = new CreateItemTask();
        mCreateItemTask.execute(item);
    }

    /**
     * Implementation of AsyncTask, to send item to the server in the background away from
     * the UI thread and get the item URL generated by the server.
     */
    private class CreateItemTask extends AsyncTask<Item2, Void, String> {
        // Screen orientation. Save and disable screen rotation in order to prevent screen rotation destroying the activity and the AsyncTask.
        private int screenOrientation;

        @Override
        protected void onPreExecute() {
            // Disable screen rotation
            screenOrientation = getActivity().getRequestedOrientation();
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        // Return the item URL generated by the server.
        // Return null when failed
        @Override
        protected String doInBackground(Item2... items) {
            // Deal with one image at a time
            if (items.length < 1) {
                Log.e(LOG_TAG, "No specified item");
                return null;
            }
            try {
                // Upload
                String id = sendAnItem(items[0]);
                if (id == null) {
                    Log.e(LOG_TAG, "Create the item failed");
                    return null;
                }
                return id;
            } catch (IOException e) {
                Log.e(LOG_TAG, getString(R.string.connection_error));
                return null;
            }
        }

        /**
         * Uses the logging framework to display the output of the fetch
         * operation in the log fragment.
         */
        @Override
        protected void onPostExecute(String itemUrl) {
            if (itemUrl == null) {
                Toast.makeText(getActivity(), R.string.create_failed_warning, Toast.LENGTH_LONG).show();
                changeState(CreateItemState.READY_TO_SEND);
            } else {
                // Create item successfully
                Log.d(LOG_TAG, "Finish creating an item");
                changeState(CreateItemState.FINISHED);
            }

            // Enable screen rotation
            getActivity().setRequestedOrientation(screenOrientation);
        }

        // Send an item to Google App Engine
        // Return the URL of the uploaded item
        // Return null if failed
        private String sendAnItem(Item2 item) throws IOException {
            URL url = new URL(CREATE_ITEM_URL);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            int size;
            byte[] data;
            OutputStream out;
            String itemUrl = null;

            // Set authentication instance ID
            urlConnection.setRequestProperty(MyConstants.HTTP_HEADER_INSTANCE_ID, InstanceID.getInstance(getActivity()).getId());
            // Set content type
            urlConnection.setRequestProperty("Content-Type", "application/json");

            try {
                // To upload data to a web server, configure the connection for output using setDoOutput(true). It will use POST if setDoOutput(true) has been called.
                urlConnection.setDoOutput(true);

                // Convert item to JSON string
                data = item.toJSONObject().toString().getBytes();
//                data = new Gson().toJson(item).getBytes();

                // Vernon debug
                Log.i(LOG_TAG, urlConnection.getRequestMethod() + " " + CREATE_ITEM_URL + " " + new String(data));

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

                // Set timeout
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);

                // Send and get response
                // getResponseCode() will automatically trigger connect()
                int responseCode = urlConnection.getResponseCode();
                String responseMsg = urlConnection.getResponseMessage();
                Log.i(LOG_TAG, "Response " + responseCode + " " + responseMsg);
                if (responseCode != HttpURLConnection.HTTP_CREATED) {
                    return null;
                }

                // Get image URL
                itemUrl = urlConnection.getHeaderField("Location");
                Log.d(LOG_TAG, "Item URL " + itemUrl);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }

            return itemUrl;
        }
    }

    private void sendOrCancel() {
        switch (mState) {
            case INITIAL:
                navigateUp();
                break;
            case UPLOADING_IMAGE:
                changeState(CreateItemState.INITIAL);
                break;
            case READY_TO_SEND:
                // Send metadata to GAE
                createItem();
                break;
        }
    }

    private void changeState(CreateItemState state) {
        // Vernon debug
        Log.d(LOG_TAG, "Change state " + mState + " -> " + state);
        mState = state;
        switch (state) {
            case INITIAL:
                // Set UI
                Picasso.with(getActivity()).load("http://aliza-1148.appspot.com.storage.googleapis.com/ImageFrame.png").into(mImageView);
                mButtonCamera.setEnabled(true);
                mButtonGallery.setEnabled(true);
                mTextViewInfo.setText(R.string.select_or_take_a_photo);
                mButtonMore.setEnabled(true);
                mButtonSend.setText(R.string.cancel);
                // Set thread
                if (mUploadImageTask != null && mUploadImageTask.getStatus() != AsyncTask.Status.FINISHED) {
                    mUploadImageTask.cancel(false);
                }
                if (mCreateItemTask != null && mCreateItemTask.getStatus() != AsyncTask.Status.FINISHED) {
                    mCreateItemTask.cancel(false);
                }
                break;
            case UPLOADING_IMAGE:
                // Set UI
                mButtonCamera.setEnabled(false);
                mButtonGallery.setEnabled(false);
                mTextViewInfo.setText(String.format(getString(R.string.uploading_image), 0));
                mButtonMore.setEnabled(true);
                mButtonSend.setText(R.string.cancel);
                break;
            case READY_TO_SEND:
                // Set UI
                mButtonCamera.setEnabled(true);
                mButtonGallery.setEnabled(true);
                mTextViewInfo.setText(R.string.wait_for_1_friend);
                mButtonMore.setEnabled(true);
                mButtonSend.setText(R.string.send);
                mButtonSend.setEnabled(true);
                // Set thread
                if (mCreateItemTask != null && mCreateItemTask.getStatus() != AsyncTask.Status.FINISHED) {
                    mCreateItemTask.cancel(false);
                }
                break;
            case SENDING:
                // Set UI
                mButtonCamera.setEnabled(false);
                mButtonGallery.setEnabled(false);
                mTextViewInfo.setText(R.string.sending);
                mButtonMore.setEnabled(false);
                mButtonSend.setText(R.string.send);
                mButtonSend.setEnabled(false);
                break;
            case FINISHED:
                showFinishDialog();
                // Delete photo if it's took by this APP
                deleteMyPhoto();
                break;
            default:
                Log.d(LOG_TAG, "Undefined state " + state);
                break;
        }
    }

    private void navigateUp() {
        getActivity().getFragmentManager().popBackStack();
    }

    private void showFinishDialog() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.power_safe_warning)
                .setTitle(R.string.create_item_successfully);
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

}
