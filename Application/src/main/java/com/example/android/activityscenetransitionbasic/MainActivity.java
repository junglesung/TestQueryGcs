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

import com.melnykov.fab.FloatingActionButton;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

/**
 * Our main Activity in this sample. Displays a grid of items which an image and title. When the
 * user clicks on an item, {@link DetailActivity} is launched, using the Activity Scene Transitions
 * framework to animatedly do so.
 */
public class MainActivity extends Activity {
    private static final String LOG_TAG = "Test";

    private SwipeRefreshLayoutBasicFragment fragment;
    FloatingActionButton fab;
    GridView mGridView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid);

        // Set swipe to refresh fragment
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            fragment = new SwipeRefreshLayoutBasicFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Set floating button attach to grid view at the first time
        if (mGridView == null) {
            // Make floating button show and hide according to GridView scroll
            fab = (FloatingActionButton) findViewById(R.id.fab);
            mGridView = fragment.getmGridView();
            if (fab == null) {
                Log.e(LOG_TAG, "fab is null");
            } else {
                Log.d(LOG_TAG, "fab is not null");
            }
            if (mGridView == null) {
                Log.e(LOG_TAG, "mGridView is null");
            } else {
                Log.d(LOG_TAG, "mGridView is not null");
            }
            if (fab != null && mGridView != null) {
                fab.attachToListView(mGridView);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        createItem();
                    }
                });
            }
        }
    }

    private void createItem() {
        //
        Toast.makeText(this, "createItem()", Toast.LENGTH_SHORT).show();
        Log.d(LOG_TAG, "createItem()");
    }
}
