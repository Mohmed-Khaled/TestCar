package eg.edu.asu.eng.walle.testcar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class ButtonsActivity extends AppCompatActivity {
    BluetoothAdapter mBluetoothAdapter;
    private Button connectButton, ForwardButton, BackButton, LeftButton, RightButton
            ,FleftBUTTON, FrightButton, BleftButton, BrightButton, LineTrackinEn, LineTrackingDis;

    private Boolean Forward = false, Back = false, Left = false, Right = false,
            FL = false, FR = false, BL = false, BR = false, lineTracking = false, stopTracking = false;

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    boolean connected = false;
    boolean foundBT = false;
    boolean openedBT = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buttons);
        if (getSupportActionBar() != null)
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionBar)));

        connectButton = (Button) findViewById(R.id.connectButton);
        ForwardButton = (Button) findViewById(R.id.buttonF);
        BackButton = (Button) findViewById(R.id.buttonB);
        LeftButton = (Button) findViewById(R.id.buttonL);
        RightButton = (Button) findViewById(R.id.buttonR);

        FleftBUTTON = (Button) findViewById(R.id.buttonFL);
        FrightButton = (Button) findViewById(R.id.buttonFR);
        BleftButton = (Button) findViewById(R.id.buttonBL);
        BrightButton = (Button) findViewById(R.id.buttonBR);

        LineTrackinEn = (Button) findViewById(R.id.EnLineTracking);
        LineTrackingDis = (Button) findViewById(R.id.DisLineTracking);

        ForwardButton.setOnTouchListener(new MyTouchListener());
        BackButton.setOnTouchListener(new MyTouchListener());
        LeftButton.setOnTouchListener(new MyTouchListener());
        RightButton.setOnTouchListener(new MyTouchListener());

        FleftBUTTON.setOnTouchListener(new MyTouchListener());
        FrightButton.setOnTouchListener(new MyTouchListener());
        BleftButton.setOnTouchListener(new MyTouchListener());
        BrightButton.setOnTouchListener(new MyTouchListener());

        LineTrackinEn.setOnClickListener(new StartTrackingListener());
        LineTrackingDis.setOnClickListener(new StopTrackingListener());
    }

    @Override
    protected void onPause() {
        if (connected) {
            try {
                closeBT();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                connectButton.setText("Connect");
                connected = false;
            }
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_buttons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_accelerometer:
                startActivity(new Intent(this, AccelerometerActivity.class));
                return true;
            case R.id.action_full_control:
                startActivity(new Intent(this, FullControlActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void connect(View v) {
        if (!connected) {
            try {
                foundBT = findBT();
                if (foundBT)
                    openBT();
                else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Device not found", Toast.LENGTH_LONG);
                    toast.show();
                }
            } catch (IOException ex) {ex.printStackTrace();}
            if (openedBT) {
                connectButton.setText("Disconnect");
                connected = true;
            }
        } else {
            try {
                closeBT();
            } catch (IOException ex) {ex.printStackTrace();}
            finally {
                connectButton.setText("Connect");
                connected = false;
            }
        }
    }

    public Boolean findBT() {
        Log.i("findBT", "in findBT");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.i("findBT", "bt adapter set");

        if (mBluetoothAdapter == null) {
            Log.i("findBT", "null");
            Toast toast = Toast.makeText(getApplicationContext(), "No Bluetooth Adapter Available", Toast.LENGTH_LONG);
            toast.show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.i("findBT", "paired dev: " + pairedDevices.toString());

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equalsIgnoreCase(getString(R.string.deviceName))) {
                    Log.i("findBT", "found seed");
                    mmDevice = device;
                    Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Device Found", Toast.LENGTH_LONG);
                    toast.show();
                    return true;
                }
            }
        }
        Log.i("findBT", "could not fine seed");
        return false;
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        Log.i("openBT", "after createRf");

        mmSocket.connect();
        Log.i("openBT", "after connect");
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

  //    beginListenForData();
        openedBT = true;
        Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Opened", Toast.LENGTH_LONG);
        toast.show();
    }

    void closeBT() throws IOException {
        sendChar(Utility.STOP);
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        openedBT = false;
        Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Closed", Toast.LENGTH_LONG);
        toast.show();
    }

    //not useful now
    void beginListenForData() {
        final byte delimiter = 10; //This is the ASCII code for a newline character
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
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
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

    public class MyTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && lineTracking == false) {
                // PRESSED
                setBoolean(view,true);
                view.setBackgroundResource(R.drawable.green_button);
            } else {
                // RELEASED
                setBoolean(view,false);
                view.setBackgroundResource(R.drawable.blue_button);
            }
            controlCar();
            return true;
        }
    }

    public class StopTrackingListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            reset();
            try {
                sendChar(Utility.STOP_TRACKING);

            } catch (IOException e) {
                e.printStackTrace();
            }
            controlCar();
        }
    }
    public class StartTrackingListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            reset();
            try {
                sendChar(Utility.START_TRACKING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setBoolean(View v, Boolean bool){
        if(v.getId() == R.id.buttonF){
            Forward = bool;
        }
        else if(v.getId() == R.id.buttonB){
            Back = bool;
        }
        else if(v.getId() == R.id.buttonL){
            Left = bool;
        }
        else if(v.getId() == R.id.buttonR){
            Right = bool;
        }
        else if(v.getId() == R.id.buttonFL){
            FL = bool;
        }
        else if(v.getId() == R.id.buttonFR){
            FR = bool;
        }
        else if(v.getId() == R.id.buttonBL){
            BL = bool;
        }
        else if(v.getId() == R.id.buttonBR){
            BR = bool;
        }
    }

    public void controlCar() {
        if (connectButton.getText().equals("Disconnect")) {
            try {
                if (Forward) {
                    sendChar(Utility.FORWARD);
                } else if (Back) {
                    sendChar(Utility.BACK);
                } else if (Left) {
                    sendChar(Utility.LEFT);
                } else if (Right) {
                    sendChar(Utility.RIGHT);
                } else if (FL) {
                    sendChar(Utility.FORWARD_LEFT);
                } else if (FR) {
                    sendChar(Utility.FORWARD_RIGHT);
                } else if (BL) {
                    sendChar(Utility.BACK_LEFT);
                } else if (BR) {
                    sendChar(Utility.BACK_RIGHT);
                } else {
                    sendChar(Utility.STOP);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendChar(String Char) throws IOException {
        try {
            Utility.sendData(Char, mmOutputStream, getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reset(){
        Forward = false;
        Back = false;
        Left = false;
        Right = false;
        FL = false;
        FR = false;
        BL = false;
        BR = false;
    }
}
