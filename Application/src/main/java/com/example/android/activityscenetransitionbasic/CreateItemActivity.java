package com.example.android.activityscenetransitionbasic;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class CreateItemActivity extends Activity {

    // The request code to invoke a camera APP
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView mImageView;
    private ImageButton mButtonCamera;
    private ImageButton mButtonGallery;
    private Button mButtonMore;
    private Button mButtonSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        mImageView = (ImageView) findViewById(R.id.imageview_create_item);
        if (mImageView != null) {
            Picasso.with(this)
                    .load("http://testgcsserver.appspot.com.storage.googleapis.com/image_frame.png")
                    .into(mImageView);
        }

        mButtonCamera = (ImageButton) findViewById(R.id.imagebutton_camera);
        mButtonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        mButtonGallery = (ImageButton) findViewById(R.id.imagebutton_gallery);
        mButtonMore = (Button) findViewById(R.id.button_create_item_more);
        mButtonSend = (Button) findViewById(R.id.button_create_item_send);
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
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
            // TODO:
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
}
