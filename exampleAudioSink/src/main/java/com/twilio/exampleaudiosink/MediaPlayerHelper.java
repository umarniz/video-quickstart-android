package com.twilio.exampleaudiosink;

import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

class MediaPlayerHelper {

    private MediaPlayer player;
    private boolean isReleased = false;
    private MediaPlayerListener listener;
    MediaPlayerHelper() {
    }

    boolean isPlaying() {
        if (player == null || isReleased) {
            return false;
        }
        return player.isPlaying();
    }

    void playFile(String path, final MediaPlayerListener listener) throws IOException {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setDataSource(path);
        player.prepare();
        player.start();
        player.setOnCompletionListener(mp -> {
            player.release();
            player = null;
            isReleased = true;
            listener.onTrackFinished();
        });

        isReleased = false;
    }

    public boolean stopPlaying() {
        if (player == null) return false;
        if (!player.isPlaying()) return false;
        player.stop();
        player.release();
        isReleased = true;
        return true;
    }

    interface MediaPlayerListener {
        void onTrackFinished();
    }
}
