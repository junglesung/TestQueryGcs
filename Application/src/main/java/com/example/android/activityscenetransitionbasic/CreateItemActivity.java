package com.example.android.activityscenetransitionbasic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

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

public class CreateItemActivity extends Activity {

    private static final String LOG_TAG = "Test";

    // The request code to invoke a camera APP
    static final int REQUEST_IMAGE_CAPTURE = 1;
    // The request code to choose an image
    private final int REQUEST_IMAGE_BROWSE = 2;
    // URL
    private final String UPLOAD_IMAGE_URL = "https://testgcsserver.appspot.com/api/0.1/storeImage";
    private final String CREATE_ITEM_URL = "https://testgcsserver.appspot.com/api/0.1/items";

    // Current image path
    Uri mCurrentPhotoUri;
    String mGcsPhotoUrl;

    // State
    CreateItemState mState;

    // Threads
    UploadImageTask mUploadImageTask;
    CreateItemTask mCreateItemTask;

    private ImageView mImageView;
    private ImageButton mButtonCamera;
    private ImageButton mButtonGallery;
    private Button mButtonMore;
    private Button mButtonSend;
    private TextView mTextViewInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        mImageView = (ImageView) findViewById(R.id.imageview_create_item);

        mButtonCamera = (ImageButton) findViewById(R.id.imagebutton_camera);
        mButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        mButtonGallery = (ImageButton) findViewById(R.id.imagebutton_gallery);
        mButtonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseAnImage();
            }
        });

        mButtonMore = (Button) findViewById(R.id.button_create_item_more);
        mButtonMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date d = new Date();
                JSONObject a = new JSONObject();
                try {
                    a.put("time", d);
                } catch (JSONException e) {
                    e.printStackTrace();
                    a = null;
                }
                if (a != null) {
                    Log.d(LOG_TAG, a.toString());
                } else {
                    Log.d(LOG_TAG, "a is null");
                }
            }
        });

        mButtonSend = (Button) findViewById(R.id.button_create_item_send);
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOrCancel();
            }
        });

        mTextViewInfo = (TextView) findViewById(R.id.textview_information);

        changeState(CreateItemState.INITIAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_item, menu);
        return true;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // The ACTION_GET_CONTENT intent was sent with the request code
        // START_ACTIVITY_ID_CHOOSE_AN_IMAGE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            onActivityResultImageCapture(data);
        } else if (requestCode == REQUEST_IMAGE_BROWSE && resultCode == Activity.RESULT_OK) {
            onActivityResultImageBrowse(data);
        }
    }

    private void onActivityResultImageCapture(Intent data) {
        // Display image
        Picasso.with(this).load(mCurrentPhotoUri).into(mImageView);
        galleryAddPic();
        uploadImage();
    }

    private void onActivityResultImageBrowse(Intent data) {
        // The document selected by the user won't be returned in the intent.
        // Instead, a URI to that document will be contained in the return intent
        // provided to this method as a parameter.
        // Pull that URI using resultData.getData().
        if (data != null) {
            mCurrentPhotoUri = data.getData();
            Log.i(LOG_TAG, "Chosen image URI " + mCurrentPhotoUri.toString());
            Picasso.with(this).load(mCurrentPhotoUri).into(mImageView);
            uploadImage();
        }
    }

    private void uploadImage() {
        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // Change state
            changeState(CreateItemState.UPLOADING_IMAGE);
            // Execute uploading thread
            mUploadImageTask = new UploadImageTask();
            mUploadImageTask.execute(mCurrentPhotoUri);
        } else {
            Toast.makeText(this, getString(R.string.no_network_connection_available), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Implementation of AsyncTask, to fetch the data in the background away from
     * the UI thread.
     */
    private class UploadImageTask extends AsyncTask<Uri, Integer, String> {

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
            if (url == null) {
                changeState(CreateItemState.INITIAL);
            } else {
                // Upload image successfully
                Log.d(LOG_TAG, "Finish uploading an image");
                changeState(CreateItemState.READY_TO_SEND);
            }
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
            String imageUrl = null;

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
                in = new BufferedInputStream(getContentResolver().openInputStream(uri));
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
                Log.d(LOG_TAG, "Image URL " + imageUrl);

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
        Cursor cursor = this.getContentResolver().query(uri, null, null, null, null, null);

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
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Log.e(LOG_TAG, getString(R.string.no_camera_app));
            Toast.makeText(this, getString(R.string.no_camera_app), Toast.LENGTH_SHORT).show();
            return;
        }
        // Ensure external storage is writable
        if (!isExternalStorageWritable()) {
            Log.e(LOG_TAG, getString(R.string.external_storage_is_not_writable));
            Toast.makeText(this, getString(R.string.external_storage_is_not_writable), Toast.LENGTH_SHORT).show();
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

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "AAA");
        if (!storageDir.isDirectory() && !storageDir.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
            return null;
        }
        File image = new File(storageDir, imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoUri = Uri.fromFile(image);

        // Vernon debug
        Log.d(LOG_TAG, "Image " + mCurrentPhotoUri.toString());

        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoUri.getPath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
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
        PackageManager packageManager = this.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
        if (list.size() > 0) {
            // 如果有可用的Activity
            // 使用Intent Chooser
            Intent destIntent = Intent.createChooser(intent, "選取圖片");
            startActivityForResult(destIntent, REQUEST_IMAGE_BROWSE);
        } else {
            // 沒有可用的Activity
            Toast.makeText(this, getString(R.string.no_app_to_choose_an_image), Toast.LENGTH_SHORT).show();
        }
    }

    private void createItem() {
        // Check network connection ability and then access Google Cloud Storage
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // Prepare event data
            Item2 item = new Item2(null, 2, 1, mGcsPhotoUrl);
            // Change state
            changeState(CreateItemState.SENDING);
            // Execute uploading thread
            mCreateItemTask = new CreateItemTask();
            mCreateItemTask.execute(item);
        } else {
            Toast.makeText(this, getString(R.string.no_network_connection_available), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Implementation of AsyncTask, to send item to the server in the background away from
     * the UI thread and get the item URL generated by the server.
     */
    private class CreateItemTask extends AsyncTask<Item2, Void, String> {

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
                Toast.makeText(getApplicationContext(), R.string.create_failed_warning, Toast.LENGTH_LONG).show();
                changeState(CreateItemState.READY_TO_SEND);
            } else {
                // Create item successfully
                Log.d(LOG_TAG, "Finish creating an item");
                changeState(CreateItemState.FINISHED);
            }
        }

        // Send an image to Google Cloud Storage
        // Return the URL of the uploaded image
        // Return null if failed
        private String sendAnItem(Item2 item) throws IOException {
            URL url = new URL(CREATE_ITEM_URL);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            int size;
            byte[] data;
            OutputStream out;
            String itemUrl = null;

            // Set content type
            urlConnection.setRequestProperty("Content-Type", "application/json");

            try {
                // To upload data to a web server, configure the connection for output using setDoOutput(true). It will use POST if setDoOutput(true) has been called.
                urlConnection.setDoOutput(true);

                // Convert item to JSON string
                data = item.toJSONObject().toString().getBytes();

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
                    Log.d(LOG_TAG, "Sending item canceled");
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
                changeState(CreateItemState.FINISHED);
                break;
            case UPLOADING_IMAGE:
                changeState(CreateItemState.INITIAL);
                break;
            case READY_TO_SEND:
                // Send metadata to GAE
                createItem();
                break;
            case SENDING:
                // Cancel sending
                // mSendTask.cancel(false);
                // Testing only, TODO: move to onPostExecute() in the future
                changeState(CreateItemState.READY_TO_SEND);
                break;
        }
    }

    enum CreateItemState {
        INITIAL, UPLOADING_IMAGE, READY_TO_SEND, SENDING, FINISHED
    }

    private void changeState(CreateItemState state) {
        // Vernon debug
        Log.d(LOG_TAG, "Change state " + mState + " -> " + state);
        mState = state;
        switch (state) {
            case INITIAL:
                // Set UI
                Picasso.with(this).load("http://testgcsserver.appspot.com.storage.googleapis.com/ImageFrame.png").into(mImageView);
                mButtonCamera.setEnabled(true);
                mButtonGallery.setEnabled(true);
                mTextViewInfo.setText(R.string.select_or_take_a_photo);
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
                mButtonSend.setText(R.string.cancel);
                break;
            case READY_TO_SEND:
                // Set UI
                mButtonCamera.setEnabled(true);
                mButtonGallery.setEnabled(true);
                mTextViewInfo.setText(R.string.wait_for_1_friend);
                mButtonSend.setText(R.string.send);
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
                mButtonSend.setText(R.string.cancel);
                break;
            case FINISHED:
                showFinishDialog();
                NavUtils.navigateUpFromSameTask(this);
                break;
            default:
                Log.d(LOG_TAG, "Undefined state " + state);
                break;
        }
    }

    private void showFinishDialog() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.power_safe_warning)
                .setTitle(R.string.create_item_successfully);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        // 4. Show the dialog
        dialog.show();
    }
}
