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

package it.imwatch.bluetooth.demo.callbacks;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import it.imwatch.bluetooth.api.adapters.SerialPortProfileServer;
import it.imwatch.bluetooth.api.interfaces.SerialPortServerCallbacks;
import it.imwatch.bluetooth.demo.EventLogActivity;

/**
 * Used to receive callbacks from the Bluetooth stacks.
 * These method are automatically called whenever some data is sent by the remote device,
 * if the device disconnects, or if a client connects to the i'm Watch.
 * <p/>
 * Holds a Handler instance for notifying the UI.
 */
public class ServerCallbacks implements SerialPortServerCallbacks {

    Handler mHandler;

    public ServerCallbacks(Handler handler) {
        mHandler = handler;
    }

    /**
     * Called when a new client has connected to the related {@link SerialPortProfileServer}.
     *
     * @param address MAC Address of the client.
     */
    @Override
    public void onClientConnected(String address) {
        Message msg = mHandler.obtainMessage();

        // Connected
        msg.what = EventLogActivity.CONNECTED;

        Bundle b = new Bundle();
        b.putString("address", address);
        msg.setData(b);

        mHandler.sendMessage(msg);
    }

    /**
     * Called when the client sends data to the server.
     *
     * @param dataLength Length in bytes of received data
     */
    @Override
    public void onDataReceived(int dataLength) {
        Message msg = mHandler.obtainMessage();

        // Data received
        msg.what = EventLogActivity.DATA_RECEIVED;
        msg.arg1 = dataLength;

        mHandler.sendMessage(msg);
    }

    /** Called when the client has disconnected. */
    @Override
    public void onClientDisconnected() {
        // Disconnected
        mHandler.sendEmptyMessage(EventLogActivity.DISCONNECTED);
    }
}
