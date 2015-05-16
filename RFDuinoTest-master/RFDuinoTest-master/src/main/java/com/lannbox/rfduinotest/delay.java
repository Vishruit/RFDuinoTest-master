package com.lannbox.rfduinotest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;


public class delay extends Activity {

    private final RFduinoService rfduino;
    public static String[] players = {"EA:02:7F:9E:5F:7C", "EC:C7:D5:05:67:BF"};

    public delay(RFduinoService rfduino) {
        this.rfduino = rfduino;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_random);
        int t = 0;

        for(int i =0; i<10; i++) {

            rfduino.disconnect();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms

                }
            }, 5000);

            rfduino.connect(players[t]);

            t = 1-t;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }
}
