package com.lannbox.rfduinotest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.UUID;


public class Final extends Activity {

    boolean scanStarted;
    int c = 0;
    protected cBaseApplication app;
    int counter, count;
    final Handler handler = new Handler();
    int t = 0;
    BallBounce ourBall;
    Button test;
    int screenTouch;
    TextView tvA, tvB;
    TextView tvPlayerA, tvPlayerB;
    private Handler handler1 = new Handler();
    private int[] score ={0, 0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final);

        app = (cBaseApplication)getApplication();
        ourBall = new BallBounce(app);

        tvA = (TextView)findViewById(R.id.tvA);
        tvB = (TextView)findViewById(R.id.tvB);
        tvPlayerA = (TextView)findViewById(R.id.tvPlayerA);
        tvPlayerB = (TextView)findViewById(R.id.tvPlayerB);

        mConnect();

        test = (Button)findViewById(R.id.btnTest);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                count++;
                if (count!=0)
                    swap();

                if (cBaseApplication.touchData.equals("01") ||
                        cBaseApplication.touchData.equals("02") ||
                        cBaseApplication.touchData.equals("03") ||
                        screenTouch == 1 ||
                        screenTouch == 2){
                    if(app.currentPlayer == 1) {
                        tvA.setText("Working!");
                        tvPlayerA.setBackgroundColor(Color.GREEN);
                    }
                    else if(app.currentPlayer == 0) {
                        tvB.setText("Working!");
                    }

                }
                else{
                    if(app.currentPlayer == 1)
                        tvA.setText("Touch Any Board");
                    else if(app.currentPlayer == 0)
                        tvB.setText("Touch Any Board");
                }
            }
        });

        final Button switch1 = (Button)findViewById(R.id.button2);
        switch1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                app.rfduinoService.disconnect();
                app.currentPlayer = 1-app.currentPlayer;
                while(!app.rfduinoService.connect(cBaseApplication.players[app.currentPlayer])) {
                    counter++;
                    Log.d("while loop", String.valueOf(counter));
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms
                        if(app.state >= cBaseApplication.STATE_CONNECTING) {
                            setContentView(ourBall);
                        }
                        else
                            reconnect(app.currentPlayer);
                    }
                }, 1000);
                switch1.setText("Switched");

            }
        });
    }

    private void reconnect(int currentPlayer) {
        app.rfduinoService.disconnect();
        while(!app.rfduinoService.connect(cBaseApplication.players[currentPlayer])) {
            counter++;
            Log.d("while loop", String.valueOf(counter));
        }
        while(t == 0) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    if (app.state >= cBaseApplication.STATE_CONNECTING)
                        t = 1;
                    else {
                        reconnect(app.currentPlayer);
                        t=0;
                    }
                }
            }, 1000);
        }

    }

    private void mConnect() {
        scanStarted = true;
        app.bluetoothAdapter.startLeScan(
                new UUID[]{ RFduinoService.UUID_SERVICE },
                ((cBaseApplication)this.getApplicationContext()));

        while(c==0) {
            Intent rfduinoIntent = new Intent(Final.this, RFduinoService.class);
            boolean out = bindService(rfduinoIntent,
                    ((cBaseApplication)this.getApplicationContext()).rfduinoServiceConnection, BIND_AUTO_CREATE);
            Log.d("OnCreate Connect", "(bind service value)" + String.valueOf(out));
            if (out) {
                c = 1;
                break;
            }
        }
    }


    class BallBounce extends View implements View.OnTouchListener {

        Bitmap bkgr, ball;
        int screenW, screenH,ballW, ballH;
        float dY = 1, dX = 1;
        Paint paint;
        public int X, Y;
        int w, h;
        int x0Bound, x1Bound;
        boolean bounceChecker;
        int plateTouched = 0;

        public BallBounce(Context context) {
            super(context);

            ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
            bkgr = BitmapFactory.decodeResource(getResources(), R.drawable.sky1);

            ballW = ball.getWidth();
            ballH = ball.getHeight();

            Random r1= new Random();
            Random r2= new Random();
            dX=r1.nextInt(1)+1;
            dY=r2.nextInt(1)+1;

            final Rect[] rect = {new Rect((int)(screenW*.95), getTop(),getRight(),getBottom()),
                    new Rect(getLeft(), getTop(),(int)(screenW*.05),getBottom())};
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawBitmap(bkgr, 0, 0, null);
            paint = new Paint();
            paint.setColor(Color.BLACK);
            canvas.drawLine(canvas.getWidth() / 2, 0, canvas.getWidth() / 2, canvas.getHeight(), paint);
            paint.setColor(Color.RED);
            canvas.drawLine(canvas.getWidth() / 10, 0, canvas.getWidth() / 10, canvas.getHeight(), paint);
            canvas.drawLine(canvas.getWidth()*9/10, 0 , canvas.getWidth()*9/10, canvas.getHeight(), paint);

            canvas.drawBitmap(ball, X, Y, null);

            w = canvas.getWidth();
            h = canvas.getHeight();
            x0Bound = canvas.getWidth() / 10;
            x1Bound = canvas.getWidth()*9/10;  // basically width - x0Bound

            paint.setColor(Color.BLACK);
            canvas.drawText("Player 1", ((w/4 + x0Bound/2)), 50, paint);
            canvas.drawText("Player 2", ((w*3/4 - x0Bound/2)), 50, paint);

            Y += (int) dY;
            X += (int) dX;
            bounceChecker = rebounceInX(X, w, x0Bound, x1Bound);
            rebounceInY(Y, h);

// TODO            test.draw(canvas);
//            test.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    count++;
//                    if(count%2 == 0)
//                        test.setText("Testing A");
//                    else
//                        test.setText("Test");
//                }
//            });
//            test.setClickable(true);
//            test.callOnClick();


            // glowing up of rectangles upon touch
            if (cBaseApplication.touchData.equals("01") ||
                    cBaseApplication.touchData.equals("02") ||
                    cBaseApplication.touchData.equals("03") ||
                    screenTouch == 1 ||
                    screenTouch == 2){
                plateTouched = 1;
                if (app.currentPlayer == 0 || screenTouch == 2){
                    paint.setColor(Color.RED);
                    canvas.drawRect((int)(screenW*.95), getTop(), getRight(), getBottom() ,paint);
//                    screenTouch = 0;
                }
                else if (app.currentPlayer == 1 || screenTouch == 1){
                    paint.setColor(Color.RED);
                    canvas.drawRect(getLeft(), getTop(),(int)(screenW*.05),getBottom() ,paint);
//                    screenTouch = 0;
                }
            }else{
                plateTouched = 0;
            }

            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    screenTouch = 0;
                }
            }, 1000);

            scoreUpdate(canvas, paint);
//            handler1.getLooper();
//            Looper.loop();


//            if (bounceChecker && plateTouched == 1){
//                swap();
//            }
//            else{
//                dX = - dX;
//                reconnect (app.currentPlayer);
//            }
            if (bounceChecker){
                swap();
            }


            GamePlayer1(X, Y);

            invalidate();
        }

        private void rebounceInY(int y, int h) {
            if (y >= (h - ballH) || y <= 0)
                dY = -1 * dY;
        }

        private boolean rebounceInX(int x, int w, int x0Bound, int x1Bound) {
            if (x >= (w - ballW - x0Bound) || x <= (0 +x0Bound)) {
                dX = -1 * dX;
                return true;
            }
            else
                return false;
        }

        private void GamePlayer1(int x, int y) {

        }

        private void scoreUpdate(Canvas canvas, Paint paint){
            // Score
            if(plateTouched == 1)
                if(X>(screenW - ballW)/2) {
                    score[0]++;
                }else if (X<(screenW - ballW)/2){
                    score[1]++;
                }

            // Winning Rule
            if(app.currentPlayer == 1) {
                if (score[1] == 1000) {
                    Toast.makeText(Final.this, "Player A" + " wins", Toast.LENGTH_SHORT).show();
                    dX = 0;
                    dY = 0;
                }
            }
            if(app.currentPlayer == 0)
                if (score[0] == 1000) {
                    Toast.makeText(Final.this, "Player B" + " wins", Toast.LENGTH_SHORT).show();
                    dX=0; dY=0;
                }


            // Score Draw
            String score2 = "Player B" + " \n" + String.valueOf(1000 - score[0]);
            String score1 = "Player A" + "\n" + String.valueOf(1000 - score[1]);
            canvas.drawText(score2, screenW-150, 100, paint);
            canvas.drawText(score1, 100, 100, paint);
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.d("coordinates", String.valueOf(event.getX()) + ", " + String.valueOf(event.getY()));
            switch(event.getAction()){
                case MotionEvent.ACTION_UP:
//                    screenTouch = 0;
                    Log.d("up", "inside");
                case MotionEvent.ACTION_DOWN:
                    Log.d("down", "inside");
                    if(event.getX() < screenW/2 && app.currentPlayer == 1)
                        screenTouch = 1;
                    else if (event.getX() > screenW/2 && app.currentPlayer == 0)
                        screenTouch = 2;
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d("down", "inside");
                    if(event.getX() < screenW/2 && app.currentPlayer == 1)
                        screenTouch = 1;
                    else if (event.getX() > screenW/2 && app.currentPlayer == 0)
                        screenTouch = 2;
                    break;
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent){
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.d("coordinates", String.valueOf(motionEvent.getX()) + ", " + String.valueOf(motionEvent.getY()));
            switch(motionEvent.getAction()){
                case MotionEvent.ACTION_UP:
                        screenTouch = 0;
                case MotionEvent.ACTION_DOWN:
                    Log.d("down", "inside");
                    if(motionEvent.getX() < view.getWidth()/2)
                        screenTouch = 1;
                    else
                        screenTouch = 2;
                    break;
            }
            return false;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            screenW = w;
            screenH = h;
            bkgr = Bitmap.createScaledBitmap(bkgr, w, h, true);
            X = (screenW - ballW)/2;
            Y = (screenH - ballH)/2;
        }

        public void pause() {
        }

        public void resume() {
        }


// Class ball bounce ends here
    }

    private void swap() {
        app.rfduinoService.disconnect();
        app.currentPlayer = 1-app.currentPlayer;
        while(!app.rfduinoService.connect(cBaseApplication.players[app.currentPlayer])) {
            counter++;
            Log.d("while loop", String.valueOf(counter));
        }


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                if(app.state >= cBaseApplication.STATE_CONNECTING);
                else
                    reconnect(app.currentPlayer);
            }
        }, 1000);

        // Todo while condition
//        while(t == 0) {
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    // Do something after 5s = 5000ms
//                    if (app.state >= cBaseApplication.STATE_CONNECTING)
//                        t = 1;
//                    else {
//                        reconnect(app.currentPlayer);
//                        t=0;
//                    }
//                }
//            }, 1000);
//        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        (app.getApplicationContext()).registerReceiver(((cBaseApplication) this.getApplicationContext()).scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        (app.getApplicationContext()).registerReceiver(((cBaseApplication) this.getApplicationContext()).bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        (app.getApplicationContext()).registerReceiver(((cBaseApplication) this.getApplicationContext()).rfduinoReceiver, RFduinoService.getIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();
        ((cBaseApplication) this.getApplicationContext()).bluetoothAdapter.stopLeScan(((cBaseApplication) this.getApplicationContext()));
        (app.getApplicationContext()).unbindService(app.rfduinoServiceConnection);
        (app.getApplicationContext()).unregisterReceiver(app.scanModeReceiver);
        (app.getApplicationContext()).unregisterReceiver(app.bluetoothStateReceiver);
        (app.getApplicationContext()).unregisterReceiver(app.rfduinoReceiver);
        app.bluetoothAdapter.disable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.bluetoothAdapter.disable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ourBall.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ourBall.pause();
    }
}
