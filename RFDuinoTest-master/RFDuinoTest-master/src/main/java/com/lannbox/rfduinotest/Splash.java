package com.lannbox.rfduinotest;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;


public class Splash extends Activity {

    MediaPlayer ourSong;
    @Override
    protected void onCreate(Bundle touchPad) {
        super.onCreate(touchPad);
        setContentView(R.layout.activity_splash);
        ourSong = MediaPlayer.create(Splash.this, R.raw.blue_spring);
        ourSong.start();

        Thread timer = new Thread() {
            public void run() {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Intent openActivityMain = new Intent("com.RFDuinoTest.StartPage");
                    startActivity(openActivityMain);
                }
            }
        };
        timer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ourSong.release();
        finish();
    }
}
