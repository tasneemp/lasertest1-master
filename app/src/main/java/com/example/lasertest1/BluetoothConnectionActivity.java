package com.example.lasertest1;

/**
 * Created by Ashwini on 07-03-2018.
 */
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


import static com.example.lasertest1.MainActivity.f;
import static java.util.UUID.fromString;

public class BluetoothConnectionActivity extends Activity{

    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    static String data="abc";
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
      //  this.requestWindowFeature(Window.FEATURE_NO_TITLE);
       //setContentView(R.layout.bt_back);

      /*  Button openButton=(Button)findViewById(R.id.open);
        Button sendButton = (Button)findViewById(R.id.SEND);
        Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);
        myTextbox = (EditText)findViewById(R.id.entry);
*/
        //Open Button



        try
                {
                    findBT();

                    openBT();
                   /* Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);*/
                }
                catch (Exception ex) { }
            }


        //Send Button
        /*sendButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    sendData();
                }
                catch (IOException ex) { }
            }
        });
*/
        //Close button



    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))//name of bt comes here
                {
                    mmDevice = device;
//                    myLabel.setText("Bluetooth Device Found");
                    break;
                }
            }

        }

    }

    void openBT() throws IOException
    {

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //of the device to check

        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        Log.d("bt connect",uuid.toString());
        mmSocket.connect();
        Log.d("bt connect",mmSocket.toString());
        mmInputStream = mmSocket.getInputStream();
//        myLabel.setText("Bluetooth Opened");
        beginListenForData();

        //else {myTextbox.setText("wrong uuid");}
    }

    void beginListenForData() {
        Log.d("f before bt",String.valueOf(f));

        final Handler handler = new Handler();
        final byte delimiter = 59; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int bytesAvailable = mmInputStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;
                                        // if(data.equals("example"))
                                        handler.post(new Runnable() {
                                            public void run() {
//                                                myTextbox.setText(data);
                                                Log.d("read data", data);
                                            }
                                        });
                                        if (data.equals("example")) {
                                            startService(new Intent(BluetoothConnectionActivity.this, CamService.class));
                                            f = 1;
                                            Log.d("f in btact", String.valueOf(f));
                                            closeBT();
                                            //moveTaskToBack(true);
                                           finish();

                                                break;
                                        }

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            stopWorker = true;
                        }
                    }
                }
            });

            workerThread.start();

        }

    /*
    void sendData() throws IOException
    {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }

*/
    @Override
    public void onResume(){
       finish();super.onResume();}
    void closeBT() throws IOException
    {
        stopWorker = true;
//        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();

//        myLabel.setText("Bluetooth Closed");
    }
}