package com.unitvectory.pushtospeech.client;

import java.util.HashMap;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * The GCM Intent Service
 * 
 * @author Jared Hatfield
 * 
 */
public class GcmIntentService extends IntentService implements OnInitListener {

    public static final int NOTIFICATION_ID = 1;

    private TextToSpeech tts;

    private ProgressMonitor monitor;

    private boolean initializing;

    private Object lock;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.initializing = true;
        this.lock = new Object();
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int code) {
        if (code == TextToSpeech.SUCCESS) {
            this.monitor = new ProgressMonitor();
            tts.setOnUtteranceProgressListener(this.monitor);
        } else {
            tts = null;
        }

        synchronized (this.lock) {
            this.initializing = false;
            this.lock.notifyAll();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            /*
             * Filter messages based on message type. Since it is likely that
             * GCM will be extended in the future with new message types, just
             * ignore any message types you're not interested in, or that you
             * don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
                    .equals(messageType)) {
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
                    .equals(messageType)) {
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
                    .equals(messageType)) {
                // Wait for the service to initialize
                synchronized (this.lock) {
                    if (this.initializing) {
                        try {
                            this.lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                // We are going to be speaking
                if (extras.getString("speak") != null) {

                    // We are going to start speaking
                    this.monitor.startSpeak();

                    // Read the message aloud
                    if (tts != null) {
                        HashMap<String, String> params =
                                new HashMap<String, String>();
                        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                                "1");
                        tts.speak(extras.getString("speak"),
                                TextToSpeech.QUEUE_ADD, params);
                    }

                    // Wait
                    this.monitor.waitSpeakDone();

                }
            }
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

}
