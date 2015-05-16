package com.lannbox.rfduinotest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lannbox.rfduinotest.util.SystemUiHider;

import java.util.Random;
import java.util.UUID;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {

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


    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);





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
                        if(app.state >= cBaseApplication.STATE_CONNECTING)
                            setContentView(ourBall);
                        else
                            reconnect(app.currentPlayer);
                    }
                }, 1000);
                switch1.setText("Switched");

            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
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
            Intent rfduinoIntent = new Intent(FullscreenActivity.this, RFduinoService.class);
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

        public BallBounce(cBaseApplication context) {
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

            test.draw(canvas);
            test.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    count++;
                    if(count%2 == 0)
                        test.setText("Testing A");
                    else
                        test.setText("Test");
                }
            });
            test.setClickable(true);
            test.callOnClick();


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
                }
                else if (app.currentPlayer == 1 || screenTouch == 1){
                    paint.setColor(Color.RED);
                    canvas.drawRect(getLeft(), getTop(),(int)(screenW*.05),getBottom() ,paint);
                }
            }else{
                plateTouched = 0;
            }

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
                    screenTouch = 0;
                case MotionEvent.ACTION_DOWN:
                    Log.d("down", "inside");
                    if(event.getX() < screenW/2)
                        screenTouch = 1;
                    else
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
