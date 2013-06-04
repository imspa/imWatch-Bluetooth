/*
 * Copyright 2013 i'm Spa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.imwatch.bluetooth.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import it.imwatch.bluetooth.api.adapters.BtAdapter;
import it.imwatch.bluetooth.api.adapters.SerialPortProfileServer;
import it.imwatch.bluetooth.api.constants.Constants;
import it.imwatch.bluetooth.api.exception.BluetoothException;
import it.imwatch.bluetooth.api.interfaces.BtAdapterCallback;
import it.imwatch.bluetooth.api.interfaces.BtProfileClient;
import it.imwatch.bluetooth.api.interfaces.BtProfileServer;
import it.imwatch.bluetooth.demo.callbacks.ClientCallbacks;
import it.imwatch.bluetooth.demo.callbacks.ServerCallbacks;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logs every event coming from the remote device. Each event is displayed in the log area
 * in the center of the screen.
 */
public class EventLogActivity extends Activity {

    /** Constants used to define the callback */
    public static final int CONNECTED = 0;
    public static final int DATA_RECEIVED = 1;
    public static final int DISCONNECTED = 2;

    private static final String TAG = EventLogActivity.class.getSimpleName();

    /**
     * UUID for the serial port.<br>
     * NOTE: It must be the same in both the i'm Watch application and the remote device application.
     */
    private static final String SERIAL_PORT_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    /** Used to receive callbacks from a new serial port client */
    Handler mHandlerClient = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case DATA_RECEIVED:
                    // Received data
                    byte[] read = readFromServer(msg.arg1);
                    appendLog("Data: " + new String(read));
                    break;

                case DISCONNECTED:
                    // Disconnected
                    appendLog("Disconnected");
                    break;

                default:
                    Log.e(TAG, "Can't handle this message");
                    break;
            }
        }
    };


    /** Used to receive callbacks from a new serial port server */
    Handler mHandlerServer = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();

            if (b == null) {
                Toast.makeText(EventLogActivity.this, "No data received", Toast.LENGTH_LONG).show();
                return;
            }
            switch (msg.what) {
                case CONNECTED:
                    // Connected
                    String address = b.getString("address");
                    appendLog("Connected: " + address);
                    break;

                case DATA_RECEIVED:
                    // Received data
                    byte[] read = readFromClient(msg.arg1);
                    appendLog("Data: " + new String(read));
                    break;

                case DISCONNECTED:
                    // Disconnected
                    appendLog("Disconnected");
                    break;

                default:
                    Log.e(TAG, "Can not handle this message");
                    break;
            }
        }
    };

    /** Log for all Bluetooth events */
    private TextView mEventLog;

    /**
     * Adapter used to communicates with the Bluetooth stack.
     * To start using Bluetooth remote calls, you must get an instance of BtAdapter by using
     * {@link BtAdapter#initializeConnection(android.content.Context,
     * it.imwatch.bluetooth.api.interfaces.BtAdapterCallback)}.
     */
    private BtAdapter mAdapter;

    /**
     * Bluetooth client instance. Holds the information of the remote server such as MAC address and remote port.
     * Once connected it can then read and write to the remote device
     */
    private BtProfileClient mClient;

    /**
     * Bluetooth server instance. In order to expose the server you must first call {@link
     * SerialPortProfileServer#accept()} (String)}
     */
    private BtProfileServer mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_log);

        mEventLog = (TextView) findViewById(R.id.event_log);
        mEventLog.setMovementMethod(new ScrollingMovementMethod());

        /*
          Button used for writing a message to the remote device
         */
        Button sayHello = (Button) findViewById(R.id.say_hello);
        sayHello.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // If mClient is not null, means that it has been created
                if (mClient != null) {
                    // Write a string to the remote device within a timeout of 1 second
                    try {
                        mClient.write("Hello!".getBytes(), Constants.TIMEOUT_ONE_SECOND);
                    }
                    catch (BluetoothException e) {
                        Log.e(TAG, "Error while sending the message to the server", e);
                        Toast.makeText(EventLogActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                // If mServer is not null, means that it has been created
                if (mServer != null) {
                    // Write a string to the remote device within a timeout of 1 second
                    try {
                        mServer.write("Hello!".getBytes(), Constants.TIMEOUT_ONE_SECOND);
                    }
                    catch (BluetoothException e) {
                        Log.e(TAG, "Error while sending the message to the client", e);
                        Toast.makeText(EventLogActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        /**
         * To get a BtAdapter instance you must first bind to the BluetoothApiService. To do so,
         * you have to register a callback and implement a method to get the BtAdapter instance.
         * To connect to the service you must define the right permission in the manifest:
         * <code>&lt;uses-permission android:name="it.imwatch.bluetooth.api.BIND_API_SERVICE"/&gt;</code>
         */
        BtAdapter.initializeConnection(this, new BtAdapterCallback() {

            @Override
            public void onConnectionSuccesfull(BtAdapter adapter) {
                mAdapter = adapter;

                // Retrieving instance value from the intent
                int instance = getIntent().getIntExtra(ConstantsDemo.BUNDLE_INSTANCE_KEY, -1);

                switch (instance) {
                    // Server
                    case 0:
                        startServer();
                        break;

                    // Client
                    case 1:
                        startClient();
                        break;

                    // No value
                    default:
                        Log.e(TAG, "No value in this intent");
                        break;
                }
            }
        });
    }

    /**
     * Starts a new Serial Port server instance. It searches for a free local port and exposes the server
     * to any clients. Obviously the remote device must be already paired (not necessarily connected) to the i'm Watch.
     */
    private void startServer() {
        // Gets a server instance from a specified UUID
        try {
            mServer = mAdapter.listenUsingRfcommWithServiceRecord(SERIAL_PORT_UUID, "Demo_Apps",
                                                                  new ServerCallbacks(mHandlerServer));
            mServer.accept();

            appendLog("Server ready");
        }
        catch (BluetoothException e) {
            Log.e(TAG, "Unable to start server. Error code: " + e.getErrorCode());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Starts a new Serial Port client instance. It retrieves the current connected device's address and
     * searches for a remote port. If found, it tries to connect.
     */
    private void startClient() {
        // Gets a client instance looking for a specified UUID in the remote device
        try {
            mClient = mAdapter.createRfcommSocketToServiceRecord(SERIAL_PORT_UUID, new ClientCallbacks(mHandlerClient));

            if (mClient != null) {
                mClient.connect();
                appendLog("Succesfully connected");
            }
            else {
                Toast.makeText(this, "Unable to find remote profile", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        catch (BluetoothException e) {
            Log.e(TAG, "Unable to start client. Error code: " + e.getErrorCode());

            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Appends a log message to the textView.
     *
     * @param message String to append
     */
    private void appendLog(String message) {
        final String data = message;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String currentDateandTime = sdf.format(new Date());

                mEventLog.append(currentDateandTime + " " + data + "\n");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ************* IMPORTANT **************
        // When closing an application, we must dispose the adapter.
        // otherwise an exception will be raised
        if (mAdapter != null) {

            try {
                mAdapter.dispose();
                // Stopping the server means that all the clients will be disconnected
                // and no other clients will be able to connect
                if (mServer != null) {
                    mServer.close();
                }

                if (mClient != null) {
                    mClient.close(true);
                }

            }
            catch (BluetoothException e) {
                Log.e(TAG, "Unable to dispose the adapter");
            }
        }
    }

    private byte[] readFromServer(int dataLength) {
        byte[] read = new byte[dataLength];
        try {
            mClient.read(read, Constants.TIMEOUT_INFINITE);
        }
        catch (BluetoothException e) {
            Toast.makeText(this,
                           "Couldn't read from the remote device. Error " + e.getErrorCode(),
                           Toast.LENGTH_LONG).show();
        }

        return read;
    }

    private byte[] readFromClient(int dataLength) {
        byte[] read = new byte[dataLength];
        try {
            mServer.read(read, Constants.TIMEOUT_INFINITE);
        }
        catch (BluetoothException e) {
            Toast.makeText(this,
                           "Couldn't read from the remote device. Error " + e.getErrorCode(),
                           Toast.LENGTH_LONG).show();
        }

        return read;
    }
}
