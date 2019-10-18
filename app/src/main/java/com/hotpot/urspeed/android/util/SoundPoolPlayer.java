package com.hotpot.urspeed.android.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.hotpot.urspeed.android.R;

public class SoundPoolPlayer {
    private SoundPool soundPool= null;
    private int beepSoundID = -1;
    private int doubleBeepSoundId = -1;
    boolean loaded = false;

    public SoundPoolPlayer(Context pContext)
    {
        this.soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            public void onLoadComplete(SoundPool soundPool, int sampleId,int status) {
                loaded = true;
            }
        });
        beepSoundID = soundPool.load(pContext, R.raw.beep01a, 1);
        doubleBeepSoundId =  soundPool.load(pContext, R.raw.beep02a, 1);
    }

    public void playBeep(){
        if(loaded) {
            this.soundPool.play(beepSoundID, 0.99f, 0.99f, 0, 0, 1);
        }
    }

    public void playDoubleBeep(){
        if(loaded) {
            this.soundPool.play(doubleBeepSoundId, 0.99f, 0.99f, 0, 0, 1);
        }
    }

    // Cleanup
    public void release() {
        // Cleanup
        if(this.soundPool != null){
            this.soundPool.release();
            this.soundPool = null;
        }

    }
}