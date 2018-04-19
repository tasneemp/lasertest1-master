package com.example.lasertest1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.bgcamera.HiddenCameraService;

import java.io.IOException;

/**
 * Created by Ashwini on 05-02-2018.
 */

public class TouchActivity extends Activity {


    /**
     * Empty activity. Its only purpose is to launch the service.
     */


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d("rchtouch","here");
                startService(new Intent(this, MainService.class));
            View v=getCurrentFocus();

            MainActivity.startThread(v);
            finish();

                }
}