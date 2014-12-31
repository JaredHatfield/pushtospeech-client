package com.unitvectory.pushtospeech.client;

import android.speech.tts.UtteranceProgressListener;

/**
 * The Progress Monitor
 * 
 * @author Jared Hatfield
 * 
 */
public class ProgressMonitor extends UtteranceProgressListener {

    private boolean busy;

    private Object lock;

    public ProgressMonitor() {
        this.lock = new Object();
    }

    @Override
    public void onDone(String arg) {
        synchronized (this.lock) {
            this.busy = false;
            this.lock.notifyAll();
        }
    }

    @Override
    public void onError(String arg) {
        synchronized (this.lock) {
            this.busy = false;
            this.lock.notifyAll();
        }
    }

    @Override
    public void onStart(String arg) {
    }

    /**
     * Flag that speech has started.
     */
    public void startSpeak() {
        synchronized (this.lock) {
            this.busy = true;
        }
    }

    /**
     * Wait until the speech is done.
     */
    public void waitSpeakDone() {
        synchronized (this.lock) {
            if (this.busy) {
                try {
                    this.lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
