BluetoothDemo
=============

**BluetoothDemo** is a simple application that shows how to develop an i'm Watch application using the i'm Watch Bluetooth API. The i'm Watch Bluetooth API gives you the ability to use the Bluetooth SPP (Serial Port Profile) to connect the i'm Watch to other devices such as smartphones or Bluetooth heart rate monitors.


## How to test BluetoothDemo using BluetoothChat

The simplest way to learn how the i'm Watch Bluetooth API works is to look at the source code of **BluetoothDemo**, a simple application that sets up a Bluetooth SPP connection, both in client and server mode. It's made to connect to the **Bluetooth Chat** sample application (found in the Android SDK samples) on an Android phone. **Bluetooth Chat** is a sample application developed by Google that shows how to use the Bluetooth SPP on an Android device.


Please remember that two applications that want to connect to each other through SPP *must* expose the same UUID, so you have to make sure of it before trying to connect **Bluetooth Chat** and **BluetoothDemo**. You must edit the **Bluetooth Chat** source code before running it, changing the UUID to match that used in **BluetoothDemo**.
Said UUID is: `00001101-0000-1000-8000-00805F9B34FB`.

You can alternatively change the UUID in **BluetoothDemo** if you want to, setting it to match that used by **BluetoothChat**.

Once you are sure that the two application expose the same UUID, you can build and install them on the i'm Watch (the **BluetoothDemo**) and on the other Android device (the **BluetoothChat**).

**NOTE: make sure that the i'm Watch and the other Android device are paired, before running BluetoothDemo!**

Once *Bluetooth Demo* is launched, the `MainActivity`  allows to open either a client or server connection:

- Press the *Start Client* button on **BluetoothDemo**'s main Activity to open a client connection to **Bluetooth Chat** on the paired Android device. A message will appear in *Bluetooth Chat* on the other Android device, to confirm the connection. You can begin exchanging messages between the two applications.

- Press the *Start Server* on **BluetoothDemo** main Activity to start a server waiting for a connection on **BluetoothDemo**. You then have to set **BluetoothChat** on the other Android device to be the client and to connect to the i'm Watch. To do so, open the *Bluetooth Chat* settings in **BluetoothChat** and choose *Connect a device - insecure*. Select the i'm Watch from the list of devices that will shows up. A proper message on **BluetoothChat** will confirm the connection; you can begin exchanging messages between the two applications.


After you've pressed either *Start Client* or *Start Server*, the `EventLogActivity` will begin showing you the exchanged messages. If you close the `EventLogActivity` (by pressing the back button) the connection (either in client or server mode) will be closed and you will be back to the `MainActivity`.

Once the two applications are connected, you can send text messages from the phone to the i'm Watch; those messages will appear in the `EventLogActivity`. You can send text from the i'm Watch to the phone pressing the *Say Hello* button which sends an "Hello!" message that will show up in **BluetoothChat** on the other Android device.




## BluetoothApi Overview

Here is an overview of the i'm Watch Bluetooth API. To see how it works in a real application look at the **BluetoothDemo** source code. For the complete reference documentation see the Javadocs.


The i'm Watch Bluetooth API work by binding to the **Bluetooth API Service**, which is a daemon running on the i'm Watch and acts as a proxy between your app and the Bluetooth stack itself, to compensate for the lack of standard Bluetooth API in Android 1.6 (on which i'm Droid is based upon).

To use the i'm Watch Bluetooth API, remember to first include the `imWatchBluetoothApi.jar` file in your project and add the permission to the application manifest:

`<uses-permission android:name="it.imwatch.bluetooth.api.BIND_API_SERVICE"/>`

***Remember that the i'm Watch Bluetooth API only works on i'm Watches running i'm Droid 2.2.0 and greater!***

The i'm Watch Bluetooth API exposes three main classes: `BtAdapter`, `BtProfileClient` and `BtProfileServer`.

`BtAdapter` represents the i'm Watch Bluetooth stack. Once you get an instance of `BtAdapter`, you can create a `BtProfileServer` or `BtProfileClient` to open up respectively a server or client connection through SPP.

The `BtAdapter` is a singleton. In order to get an instance of `BtAdapter`, you call its `initializeConnection` static method:

```java
BtAdapter.initializeConnection(this, new BtAdapterCallback() {

    @Override
    public void onConnectionSuccesfull(BtAdapter adapter) {
        // Do stuff here
    }
});
```

If the connection to the local Bluetooth API Service is successfully established, the callback `onConnectionSuccesfull` method is called, which gives you an instance of `BtAdapter` that you can use to open a server or client connection. If the connection is not established, the callback is never called. You should implement some watchdog mechanism to prevent waiting indefinitely for the connection to be established.

An proof-of-concept of such watchdog mechanism could be this:

```java
final Handler handler = new Handler();
final Runnable watchdog = new Runnable() {
	@Override
	public void run() {
		Toast.makeText(MyActivity.this, 
					   "Unable to access the Bluetooth APIs!",
					   Toast.LENGTH_SHORT)
			 .show();
	}
};

BtAdapter.initializeConnection(this, new BtAdapterCallback() {

    @Override
    public void onConnectionSuccesfull(BtAdapter adapter) {
		// Stop the watchdog, we're done!
		handler.removeAll(watchdog);
		
		// Do stuff here 
    }
});

// Start the BT API initialization timeout watchdog
handler.postDelayed(watchdog, API_CONNECTION_TIMEOUT);
```

To open a server, waiting for a connection:

```java
BtProfileServer mServer =
    mAdapter.listenUsingRfcommWithServiceRecord(
        SERIAL_PORT_UUID, "Demo_Apps",
        new ServerCallbacks(mHandlerServer));
```

To open a client connection:

```java
BtProfileClient mClient = 
    mAdapter.createRfcommSocketToServiceRecord(
        SERIAL_PORT_UUID, new ClientCallbacks(mHandlerClient));
```

The server starting method accepts three parameters. The first one is the UUID of the service you're starting, using the SPP. The second is the name the service will be exposed with on the SPP. The third argument is a callback interface implementation used to *receive* data from the connection and to get the connection status.

The client starting method accepts two parameters. The first one is the UUID of the service you're connecting to, using the SPP. The second argument is a callback interface implementation used to *receive* data from the connection and to get the connection status.

Server connections *must* implement `SerialPortServerCallbacks`, while client connections *must* implement `SerialPortClientCallbacks`.

***NOTE: An application can only open ONE server and ONE client connection at the same time***

Here is an outline of a `SerialPortServerCallbacks` implementation. The method you must override are self-explanatory and are commented below.

```java
public class ServerCallbacksImplementation implements SerialPortServerCallbacks {
    /**
     * Called when a new client has connected to the associated {@link SerialPortProfileServer}.
     * 
     * @param address MAC Address of the newly connected client 
     */
    @Override
    public void onClientConnected(String address) { ... }

    /**
     * Called when the client sends data to the server.
     *
     * @param dataLength Length in bytes of received data
     */
    @Override
    public void onDataReceived(int dataLength) { ... }

    /** Called when the client on the remote device disconnects. */
    @Override
    public void onClientDisconnected() { ... }
}
```

The client callback `SerialPortClientCallbacks` is very similar.

```java
public class ClientCallbacksImplementation implements SerialPortClientCallbacks {
	
    /**
     * Called when the client sends data to the server.
     *
     * @param dataLength Length in bytes of received data
     */
    @Override
    public void onDataReceived(int dataLength) { ... }

    /* Callback called when the client has disconnected. */
    @Override
    public void onDisconnected() { ... }
}
```

####Writing data to the serial port  

To write data on the serial port connection, both `BtProfileClient` and `BtProfileClient` provide the `write` method that accepts two parameters: the first argument is the array of bytes to write out, the second argument is a time expressed in milliseconds that specifies a waiting timeout before forcing an interruption of the execution of the writing, even if data has not been completely sent.

####Closing connection

When closing an application you *must* dispose the `BtAdapter` instance(s) you're holding, using its `dispose()` method, otherwise an exception will raised.

The `BtProfileServer` interface has two methods, `close()` and `disconnect()`, that work in a different way. The `close()` method disconnects the client (if any) and stops the server listening on the SPP. This means clients won't be able to reconnect to your profile until it's restarted. The `disconnect()` method only disconnects the connected client (if any), leaving the server running and listening for new connections.

Always remember to call the `close()` method on your profiles when you don't need them anymore!

#License
This application sources are provided under the Apache 2 license. Please check the LICENSE file for further information.