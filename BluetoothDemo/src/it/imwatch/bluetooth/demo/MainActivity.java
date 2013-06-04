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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Starts a server instance or a client instance. From the 2 buttons you can choose what
 * instance to create.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Button to start a server instance in the i'm Watch Bluetooth stack */
        Button startServer = (Button) findViewById(R.id.start_server);
        /* Button to start a client instance in the i'm Watch Bluetooth stack */
        Button startClient = (Button) findViewById(R.id.start_client);

        startServer.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EventLogActivity.class);

                // 0 for a server instance
                intent.putExtra(ConstantsDemo.BUNDLE_INSTANCE_KEY, 0);
                startActivity(intent);
            }
        });

        startClient.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EventLogActivity.class);

                // 1 for a client instance
                intent.putExtra(ConstantsDemo.BUNDLE_INSTANCE_KEY, 1);
                startActivity(intent);
            }
        });
    }
}
