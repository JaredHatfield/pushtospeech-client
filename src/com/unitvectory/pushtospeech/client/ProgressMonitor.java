package com.unitvectory.pushtospeech.client;

import android.speech.tts.UtteranceProgressListener;

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

    public void startSpeak() {
        synchronized (this.lock) {
            this.busy = true;
        }
    }

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
