//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.sampleapp;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.notifications.MessageFlags;
import com.microsoft.band.notifications.VibrationType;
import com.microsoft.band.sdk.sampleapp.notification.R;
import com.microsoft.band.tiles.BandTile;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;
import java.util.logging.SocketHandler;

import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.transport.LongPollingTransport;

public class BandNotificationAppActivity extends Activity {

	private BandClient client = null;
	private Button btnStart;
	private TextView txtStatus;
	private UUID tileId = UUID.fromString("aa0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = (TextView) findViewById(R.id.txtStatus);
		
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				txtStatus.setText("");
				new appTask().execute();
			}
		});

    }



    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private void sendSound(){
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        Intent intent = new Intent(BandNotificationAppActivity.this, NotificationReceiver.class);
//        PendingIntent pIntent = PendingIntent.getActivity(BandNotificationAppActivity.this, 0, intent, 0);
        Notification soundNotif = new Notification.Builder(this).setSound(soundUri).build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, soundNotif);

    }
    
	private class appTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    Platform.loadPlatformComponent(new AndroidPlatformComponent());
                    // Change to the IP address and matching port of your SignalR server.
                    String host = "http://divewakeweb.azurewebsites.net";
                    HubConnection connection = new HubConnection(host);
                    HubProxy hub = connection.createHubProxy("WakeHub");
                    //call hub
                    SignalRFuture<Void> awaitConnection = connection.start(new LongPollingTransport(connection.getLogger()));

//                    appendToUI("connecting to web server.");
                    try {
                        awaitConnection.get();
                        appendToUI("connected to web server.");
                    } catch (InterruptedException e) {
                        // Handle ...
                        appendToUI("connection interrupted");
                    } catch (ExecutionException e) {

                        appendToUI("exec error");
                        // Handle ...
                    }
                    //event handler: listens for data from web server
                    //hub.subscribe(this);

                    SubscriptionHandler1 handlerCon= new SubscriptionHandler1<Integer>() {
                        @Override
                        // Handler handler = new SocketHandler();
                        public void run(final Integer intensity) {
//                            appendToUI("handler invoked");
                            // Since we are updating the UI,
                            // we need to use a handler of the UI thread.
//                            appendToUI("intensity = " + intensity);
                            intensitySelect(intensity);
                            //add sound effect
                            sendSound();
                        }
                    };

                    hub.on("pulseClient",handlerCon, Integer.class);
                    //responds with sending commands

                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case DEVICE_ERROR:
                        exceptionMessage = "Please make sure bluetooth is on and the band is in range.\n";
                        break;
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    case BAND_FULL_ERROR:
                        exceptionMessage = "Band is full. Please use Microsoft Health to remove a tile.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
			return null;
		}
	}

	private void appendToUI(final String string) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	txtStatus.append(string);
            }
        });
	}
	
	private boolean addTile() throws Exception {
        /* Set the options */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap tileIcon = BitmapFactory.decodeResource(getBaseContext().getResources(), R.raw.tile_icon_large, options);
        Bitmap badgeIcon = BitmapFactory.decodeResource(getBaseContext().getResources(), R.raw.tile_icon_small, options);

		BandTile tile = new BandTile.Builder(tileId, "MessageTile", tileIcon)
			.setTileSmallIcon(badgeIcon).build();
		appendToUI("Message Tile is adding ...\n");
		if (client.getTileManager().addTile(this, tile).await()) {
			appendToUI("Message Tile is added.\n");
			return true;
		} else {
			appendToUI("Unable to add message tile to the band.\n");
			return false;
		}
	}
	
	private void sendMessage(String message) throws BandIOException {
		client.getNotificationManager().sendMessage(tileId, "Tile Message", message, new Date(), MessageFlags.SHOW_DIALOG);
		appendToUI(message + "\n");
	}
	
	private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				appendToUI("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		
		appendToUI("Band is connecting...\n");
		return ConnectionState.CONNECTED == client.connect().await();
	}

    private void intensitySelect(int select){
        try {
            switch (select){
//                case 1:
//                    // send a vibration request of type alert alarm to the Band
//                    client.getNotificationManager().vibrate(VibrationType.RAMP_UP).await();
//                    break;
                case 111:
                    // send a vibration request of type alert alarm to the Band
                    client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ALARM).await();
                    break;
                case 000:
                    // send a vibration request of type alert alarm to the Band
                    client.getNotificationManager().vibrate(VibrationType.NOTIFICATION_ONE_TONE).await();
                    break;
//                case 4:
//                    // send a vibration request of type alert alarm to the Band
//                    client.getNotificationManager().vibrate(VibrationType.ONE_TONE_HIGH).await();
//                    break;
//                case 5:
//                    // send a vibration request of type alert alarm to the Band
//                    client.getNotificationManager().vibrate(VibrationType.THREE_TONE_HIGH).await();
//                    break;
                default:
                    break;
            }
        } catch (InterruptedException e) {
// handle InterruptedException
        } catch (BandException e) {
            // handle BandException
        }
    }


}

