package eg.edu.asu.eng.walle.testcar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    Button connectButton;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    boolean connected=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectButton = (Button)findViewById(R.id.connectButton);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        mSensorManager.registerListener(this, mSensor , SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    public void connect(View v)
    {
        if (!connected)
        {
            try
            {
                findBT();
                openBT();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            finally
            {
                connectButton.setText("Disconnect");
                connected=true;
            }

        }
        else
        {
            try
            {
                closeBT();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            finally
            {
                connectButton.setText("Connect");
                connected=false;
            }
        }
    }
    public void sendFL(View v)
    {
        try {
            sendData("G");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendF(View v)
    {
        try {
            sendData("F");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendFR(View v)
    {
        try {
            sendData("I");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendL(View v)
    {
        try {
            sendData("L");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendS(View v)
    {
        try {
            sendData("S");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendR(View v)
    {
        try {
            sendData("R");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendBL(View v)
    {
        try {
            sendData("H");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendB(View v)
    {
        try {
            sendData("B");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendBR(View v)
    {
        try {
            sendData("J");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void findBT()
    {
        Log.i("findBT", "in findBT");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.i("findBT","bt adapter set");

        if(mBluetoothAdapter == null)
        {
            Log.i("findBT","null");
            Toast toast = Toast.makeText(getApplicationContext(), "No Bluetooth Adapter Available", Toast.LENGTH_LONG) ;
            toast.show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.i("findBT","paired dev: "+pairedDevices.toString());

        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equalsIgnoreCase(getString(R.string.deviceName)))
                {
                    Log.i("findBT","found seed");

                    mmDevice = device;
                    Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Device Found", Toast.LENGTH_LONG) ;
                    toast.show();
                    break;
                }
            }
        }
        Log.i("findBT","could not fine seed");
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        Log.i("openBT", "after createRf");

        mmSocket.connect();
        Log.i("openBT", "after connect");
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Opened", Toast.LENGTH_LONG) ;
        toast.show();
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            //Toast toast = Toast.makeText(getApplicationContext(),data, Toast.LENGTH_LONG) ;
                                            //toast.show();
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData(String signal) throws IOException
    {
        if (mmOutputStream != null){
            byte[] msgBuffer = signal.getBytes();
            try {
                for ( int i = 0;i < 200;i++)
                    mmOutputStream.write(msgBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Not Connected", Toast.LENGTH_LONG) ;
            toast.show();
        }
    }
    void sendAccData(String signal) throws IOException
    {
        if (mmOutputStream != null){
            byte[] msgBuffer = signal.getBytes();
            try {
                for ( int i = 0;i < 20;i++)
                    mmOutputStream.write(msgBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Not Connected", Toast.LENGTH_LONG) ;
            toast.show();
        }
    }

    void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast toast = Toast.makeText(getApplicationContext(), "Bluetooth Closed", Toast.LENGTH_LONG) ;
        toast.show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = Math.round(event.values[0]);
        float y = Math.round(event.values[1]);
        String co = "X: "+String.valueOf(x)+" Y: "+String.valueOf(y);
        Log.d("COORDIN",co);
        if (x == 0){
            try {
                sendAccData("S");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (x < -2) {
            try {
                sendAccData("F");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (x > 2) {
            try {
                sendAccData("B");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (y < -2) {
            try {
                sendAccData("L");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (y > 2) {
            try {
                sendAccData("R");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (x < -2 && y < -2) {
            try {
                sendAccData("G");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (x < -2 && y > 2) {
            try {
                sendAccData("I");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (x > 2 && y < -2) {
            try {
                sendAccData("H");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (x > 2 && y > 2) {
            try {
                sendAccData("J");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
