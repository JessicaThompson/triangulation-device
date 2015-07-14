package ca.triangulationdevice.android.util;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;

public class VolumeLevelObserver extends ContentObserver {
    private final AudioManager audioManager;
    private final Listener listener;
    private final int stream;

    public VolumeLevelObserver(Context c, Listener listener, int stream) {
        super(new Handler());
        this.audioManager = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        this.stream = stream;
        this.listener = listener;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        int currentVolume = this.audioManager.getStreamVolume(this.stream);
        this.listener.onVolumeChanged(currentVolume);
    }

    public int getCurrent() {
        return this.audioManager.getStreamVolume(this.stream);
    }

    public interface Listener {
        public void onVolumeChanged(int newVolume);
    }
}