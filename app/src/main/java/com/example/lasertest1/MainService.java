package com.example.lasertest1;

/**
 * Created by Ashwini on 05-02-2018.
 */
import android.app.Service;
import android.content.Intent;
import android.graphics.Camera;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.example.bgcamera.CameraPreview;
import com.example.bgcamera.HiddenCameraService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service{//} implements View.OnTouchListener, View.OnClickListener {
    private final String TAG = this.getClass().getName();

    private final int MIN_CLICK_TIME = 1000; //min time to launch the app
    private final int MAX_CLICK_POS = 15; //max movment to launch app
    private final float TARGET_SPEED = 10;
    private final int TARGET_PERIOD = 25;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private WindowManager.LayoutParams mTargetParams;

    private LinearLayout mControlView;
    private LinearLayout mReducedView;
    private LinearLayout mTargetView;

    private int mTargetX;
    private int mTargetY;

    private boolean mIsTouchDown;
    //private ARROW mLastInput;
    private boolean mIsFullMode;//true if the full control view is shown

//    Runnable mMoveViewRunnable = new Runnable() {
//        @Override
//        public void run() {
//            if (mIsTouchDown == true) {
//                mTargetView.postDelayed(mMoveViewRunnable, TARGET_PERIOD);
//            }
//            switch (mLastInput) {
//                case DOWN:
//                    moveDown();
//                    return;
//                case UP:
//                    moveUp();
//                    return;
//                case LEFT:
//                    moveLeft();
//                    return;
//                case RIGHT:
//                    moveRight();
//                    return;
//                default:
//                    return;
//            }
//        }
//    };

    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mIsFullMode = false;
        mIsTouchDown = false;
        proceedClick();
        Log.d("rchserv","here");
     }

    private void proceedClick() {
        Process sh = null;
        try {
            sh = Runtime.getRuntime().exec("su", null, null);
            OutputStream os = sh.getOutputStream();
            Display mdisp = mWindowManager.getDefaultDisplay();
            android.graphics.Point mdispSize = new android.graphics.Point();
            mdisp.getSize(mdispSize);
            int maxX = mdispSize.x;
            int maxY = mdispSize.y;
            Log.d("max",maxX+" "+maxY);
            int height_of_image= CameraPreview.hofimage;
            int width_of_image=CameraPreview.hofimage;
            int x_in_image=CameraPreview.x;
            int y_in_image=CameraPreview.y;
            double x=0,y=0;
            if(height_of_image!=0)
            {
                x=height_of_image-y_in_image;
                y=x_in_image;
                x = (x / height_of_image) * maxX;
                y = (y / width_of_image) * maxY;

                Log.d("hwimg",height_of_image+" "+width_of_image);
                Log.d("xyprev",x_in_image+" "+y_in_image);
                Log.d("xynew",x+" "+y);
                //callfunctionhu
            }
            int x1=(int)x;
            int y1=(int)y;
            os.write(("input tap " + x1 + " " + y1).getBytes("ASCII"));
            Log.d("touched here v1", x1 + " " + y1);

            os.flush();
            os.close();

           new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    // this code will be executed after 5 seconds
                }
            }, 5000);
            Log.d("touched here v3", y1 + " " + x1);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopSelf();
    }
}