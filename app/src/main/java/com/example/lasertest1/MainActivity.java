package com.example.lasertest1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Mat imageMat;
    private static Context context;
    //WindowManager mWindowManager;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    imageMat=new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter("intentkey1"));
        MainActivity.context = getBaseContext();

        final Button bt1=(Button)findViewById(R.id.test);
        bt1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (arg1.getAction()==MotionEvent.ACTION_DOWN)
                    Toast.makeText(MainActivity.this,"Pressed Test",Toast.LENGTH_SHORT).show();
                return true;
            }
        });

    }
    public static Context getAppContext()
    {
        return MainActivity.context;
    }

    public static void start()
    {
        Log.i("Reached start","came in start");

        Thread t = new Thread(){
            public void run(){
                context.startService(new Intent(getAppContext(),CamService.class));
            }

        };
        t.start();
    }

    public static void startThread(View v) {
        List<UUID> uuidCandidates = null;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice device = null;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device1 : pairedDevices) {
                if (device1.getName().equals("HC-05"))//name of bt comes here
                {
                    device = device1;
                    break;
                }
            }

        }
        final Server s = new Server(device, true, mBluetoothAdapter, uuidCandidates, getAppContext());
        Thread s1 = new Thread() {
            public void run() {

                try {
                    s.connect();
                } catch (Exception e) {
                }

            }
        };
        s1.start();
    }

    BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("intentkey1"))
            {
                String msg = intent.getStringExtra("key");
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                Log.e(msg, "in main activity");
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
            {
                Intent intent = new Intent(this, PreferenceSettingActivity.class);
                startActivity(intent);

                return true;
            }
            default:
            {
                return false;
            }
        }
    }


    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

}
