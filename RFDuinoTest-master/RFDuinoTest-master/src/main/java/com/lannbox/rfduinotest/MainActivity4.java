package com.lannbox.rfduinotest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Random;
import java.util.UUID;

public class MainActivity4 extends Activity implements BluetoothAdapter.LeScanCallback {
    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    private int state;

    private boolean scanStarted;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;

    private LinearLayout dataLayout;

    public static  String mickyMouse = "EA:02:7F:9E:5F:7C";  //  "EA:02:7F:9E:5F:7C";
    public static  String donaldDuck = "EC:C7:D5:05:67:BF";
    public static String touchData="110";
    int c =0, score1 = 100,score2 = 100, r=0;
    float var =1;
    int flag1 =0, flag2 = 0;

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            scanStarted &= scanning;
            Log.d ("scan mode receiver", "SMR going for UpdateUI");
            updateUi();
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                if (r==0) {
                    // To be run only once
                    if (rfduinoService.connect(donaldDuck)) {
                        upgradeState(STATE_CONNECTING);
                    }
                    r++;
//                    Toast.makeText(MainActivity4.this,"Both players are Connected", Toast.LENGTH_SHORT).show();
                }
                else if(var > 0){
                    if (rfduinoService.connect(donaldDuck)) {
                        upgradeState(STATE_CONNECTING);
                    }
                }
                else if (var<0)
                    if (rfduinoService.connect(mickyMouse)) {
                        upgradeState(STATE_CONNECTING);
                    }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };

    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                downgradeState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main_activity4);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();
        scanStarted = true;
        bluetoothAdapter.startLeScan(
                new UUID[]{ RFduinoService.UUID_SERVICE },
                MainActivity4.this);
        while(c==0) {
            Intent rfduinoIntent = new Intent(MainActivity4.this, RFduinoService.class);
            boolean out = bindService(rfduinoIntent,
                    rfduinoServiceConnection, BIND_AUTO_CREATE);
            Log.d("OnCreate Connect", "(bind service value)" + String.valueOf(out));
            if (out) {
                c = 1;
            }
        }
        setContentView(new BallBounce(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());
        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothAdapter.stopLeScan(this);
        unregisterReceiver(scanModeReceiver);
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(rfduinoReceiver);
        unbindService(rfduinoServiceConnection);
        bluetoothAdapter.disable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.disable();
    }

    private void upgradeState(int newState) {
        if (newState > state) {
            updateState(newState);
        }
    }

    private void downgradeState(int newState) {
        if (newState < state) {
            updateState(newState);
        }
    }

    private void updateState(int newState) {
        state = newState;
//        Log.d("update state","us going for update UI");
        updateUi();
    }

    private void addData(byte[] data) {
        Log.d("Road", HexAsciiHelper.bytesToHex(data));

        touchData = HexAsciiHelper.bytesToHex(data);
        if (touchData == null){
            touchData = "10";
        }
//        Log.d("addData", "addData going for update UI");
        updateUi();
    }


    private void updateUi() {
//        Log.d("UpdateUI", "Executing update UI");
        String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
            connectionText = "Connected";
        }
        Log.d("connection text", connectionText);
    }
//    private void pairDevice(BluetoothDevice device) {
//        try {
//            Log.d("pairDevice()", "Start Pairing...");
//            Method m = device.getClass()
//                    .getMethod("createBond", (Class[]) null);
//            m.invoke(device, (Object[]) null);
//            Log.d("pairDevice()", "Pairing finished.");
//        } catch (Exception e) {
//            Log.e("pairDevice()", e.getMessage());
//        }
//    }

    class BallBounce extends View {
        int screenW;
        int screenH;
        int X;
        int Y;
        int initialY;
        int ballW;
        int ballH;
        int angle;
        float dY, dX;
        int platesTouched = 0;
        Bitmap ball, bgr;
        MediaPlayer ourSong, ourSong2, touch1,touch2;

        public BallBounce(Context context) {
            super(context);
            ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball); //load a ball image
            bgr = BitmapFactory.decodeResource(getResources(), R.drawable.sky1); //load a background
            ballW = ball.getWidth();
            ballH = ball.getHeight();
            initialY = 0;
            Random r1= new Random();
            Random r2= new Random();
            Intent intent =getIntent();
            int k;
            k = intent.getIntExtra("touchValue",1);
            dX=r1.nextInt(1)+k;
            dY=r2.nextInt(1)+k;
//            platesTouched = 0;

            ourSong = MediaPlayer.create(MainActivity4.this, R.raw.alarm_clock);
            ourSong2 = MediaPlayer.create(MainActivity4.this, R.raw.buzzer2);
            touch1 = MediaPlayer.create(MainActivity4.this, R.raw.dading);
            touch2 = MediaPlayer.create(MainActivity4.this, R.raw.robotblip);
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            screenW = w;
            screenH = h;
            bgr = Bitmap.createScaledBitmap(bgr, w, h, true); //Resize background to fit the screen.
            X = (screenW / 2) - (ballW / 2); //Centre ball into the centre of the screen.
            Y=screenH / 2 - (ballH / 2);
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawBitmap(bgr, 0, 0, null);
            if(touchData!=null) {
                if (touchData.equals("00")) {
                    platesTouched = 0;
//                    Log.d("onDraw", " plates touched =0");
                } else if (touchData.equals("01")) {
                    platesTouched = 1;
//                    Log.d("onDraw", " plates touched =1");
                } else if (touchData.equals("02")) {
                    platesTouched = 2;
//                    Log.d("onDraw", " plates touched =2");
                } else if (touchData.equals("03")) {
                    platesTouched = 3;
//                    Log.d("onDraw", " plates touched =3");
                } else if (touchData.equals("110")) {
                    platesTouched = -1;
                    Toast.makeText(MainActivity4.this, "Error connecting to Rfduino : No Data", Toast.LENGTH_SHORT).show();
//                    rfduinoService.connect("EC:C7:D5:05:67:BF");
//TODO                    Log.d("onDraw", " plates touched =-1");
// TODO                   Toast.makeText(MainActivity4.this, "Attempting to reconnect",
// TODO                           Toast.LENGTH_SHORT).show();

                }
            }
//            else{
//                platesTouched=-1;
//            }

            Y += (int) dY;
            X += (int) dX;

            if(X>=(screenW)/2 && dX>0)
            {ourSong.start();}

            if(X<(screenW)/2 & dX<0)
            {ourSong2.start();}


            int t, h=0, k=0;
//            Disconnect
            if (X >= (screenW - ballW) && flag1!=-1){
                h=0;
                if(platesTouched>0){h++; }
                rfduinoService.disconnect();
                Log.d("Rebound","rebound disconnect 1");
                flag1 = -1;
//                platesTouched = -1;
            }else if(X <= 0 && flag2!=-2){
                h=0;
                if(platesTouched>0){h++; }
                rfduinoService.disconnect();
                Log.d("Rebound","rebound disconnect 2");
                flag2 = -2;
//                platesTouched = -1;
            }


            // Connect
            t = 0;
            while (flag1 == -1){
                if(h>0) {
                    var = -10;
                    Log.d("Micky connect", "connecting");
                    while (!rfduinoService.connect(mickyMouse)) {
                        Log.d("Micky connect", "not connected" + String.valueOf(t));
                        t++;
                        k++;
                        if(t==10){
                            Toast.makeText(MainActivity4.this, "Restart Micky Mouse", Toast.LENGTH_SHORT).show();
                            t=0;
                        }
                        if (k>100)
                            break;
                    }
                    upgradeState(STATE_CONNECTING);
                    flag1 = 1;
                }else{
                    var = 10;
                    rfduinoService.disconnect();
                    Log.d("Donald connect", "connecting");
                    while (!rfduinoService.connect(donaldDuck)) {
                        Log.d("donald reconnect", "not connected" + String.valueOf(t));
                        t++;
                        k++;
                        if(t==10){
                            Toast.makeText(MainActivity4.this, "Restart Donald Duck", Toast.LENGTH_SHORT).show();
                            t=0;
                            break;
                        }
                        if (k>100)
                            break;
                    }
                    upgradeState(STATE_CONNECTING);
                    flag1 = 1;
                }
            }

            t=0;
            while (flag2 == -2){
                if(h>0) {
                    var = 10;
                    Log.d("Donald connect", "connecting");
                    while (!rfduinoService.connect(donaldDuck)) {
                        Log.d("Donald connect", "not connected" + String.valueOf(t));
                        t++;
                        k++;
                        if(t==10){
                            Toast.makeText(MainActivity4.this, "Restart Donald Duck", Toast.LENGTH_SHORT).show();
                            t=0;
                            break;
                        }
                        if (k>100)
                            break;
                    }
                    upgradeState(STATE_CONNECTING);
                    flag2 = 2;
                }else{
                    var = -10;
                    rfduinoService.disconnect();
                    Log.d("Micky connect", "connecting");
                    while (!rfduinoService.connect(mickyMouse)) {
                        Log.d("Micky connect", "not connected" + String.valueOf(t));
                        t++;
                        k++;
                        if(t==10){
                            Toast.makeText(MainActivity4.this, "Restart Micky Mouse", Toast.LENGTH_SHORT).show();
                            t=0;
                        }
                        if (k>100)
                            break;
                    }
                    upgradeState(STATE_CONNECTING);
                    flag2 = 2;
                }
            }

            if (Y >= (screenH - ballH)) {
                dY = (-1) * dY; //Reverse speed when bottom hit.
            }else if(Y <= 0){
                dY = (-1) * dY;
            }
            if (X >= (screenW - ballW) && platesTouched >0) {
                dX = (-1) * dX; //Reverse speed when bottom hit.
                Log.d("Rebound","rebound");
                if(ourSong.isPlaying())
                    ourSong.pause();
            }else if(X <= 0 && platesTouched > 0){
                dX = (-1) * dX;
                Log.d("Rebound","rebound");
                if (ourSong2.isPlaying())
                    ourSong2.pause();
            }else if (X <= 0 || X > (screenW - ballW)){
                Toast.makeText(MainActivity4.this, "game over", Toast.LENGTH_SHORT).show();
                X = (screenW - ballW) / 2; //Centre ball into the centre of the screen.
                Y=screenH / 2 - (ballH / 2);
            }

            if(touchData == null) {
                platesTouched = -1;
                if(var>0)
                    rfduinoService.connect(donaldDuck);
                else if(var<0)
                    rfduinoService.connect(mickyMouse);
            }
            // The show must go on
            if(platesTouched == -1)
                if (X >= (screenW - ballW)) {
                    dX = (-1) * dX; //Reverse speed when bottom hit.
                    Log.d("Rebound","rebound auto");
                }else if(X <= 0){
                    dX = (-1) * dX;
                    Log.d("Rebound","rebound auto");
                }

            //Increase rotating angle.
            if (angle++ > 360)
                angle = 0;

            //Draw ball
            canvas.save(); //Save the position of the canvas.
            canvas.rotate(angle, X + (ballW / 2), Y + (ballH / 2)); //Rotate the canvas.
            canvas.drawBitmap(ball, X, Y, null); //Draw the ball on the rotated canvas.
            canvas.restore(); //Rotate the canvas back so that it looks like ball has rotated.

            Paint paint = new Paint(32);
            canvas.drawText("Touch Data" + touchData, 100, 80, paint);
            canvas.drawText("Plates Touched"+String.valueOf(platesTouched), 200, 120, paint);
            canvas.drawText("X=" + String.valueOf(X), 200, 60, paint);
            canvas.drawText("Y=" + String.valueOf(Y), 250, 60, paint);
            String scoreA = "PlayerA \n" + String.valueOf(1000 - score1);
            String scoreB = "PlayerB \n" + String.valueOf(1000 - score2);
            canvas.drawText(scoreA, screenW-150, 100, paint);
            canvas.drawText(scoreB, 100, 100, paint);
            paint.setColor(Color.BLACK);
            canvas.drawLine(screenW/2, 0,screenW/2, screenH,paint);

//Todo            if(X>(screenW - ballW)/2 && platesTouched>0 ){
            if(dX>0 && platesTouched>0 ){
                paint.setColor(Color.RED);
                canvas.drawRect(screenW-70, getTop(),getRight(),getBottom(),paint);
                touch1.start();
            }
//Todo            else if(X<(screenW - ballW)/2 && platesTouched>0 ){
            else if(dX<0 && platesTouched>0 ){
                paint.setColor(Color.RED);
                canvas.drawRect(getLeft(), getTop(),70,getBottom(),paint);
                touch2.start();
            }
            if(state < STATE_CONNECTING && dX>0 && X== (screenW*0.6)){
                platesTouched = -1;
                Toast.makeText(MainActivity4.this,
                        "Please Restart Player 1", Toast.LENGTH_SHORT).show();
            }else if(state < STATE_CONNECTING && dX<0 && X== (screenW*0.4)){
                platesTouched = -1;
                Toast.makeText(MainActivity4.this,
                        "Please Restart Player 2", Toast.LENGTH_SHORT).show();
            }
            if(X>(screenW - ballW)/2 && platesTouched>0 && dX>0) {
                score1++;

                if (dX < 0) {
                    scoreA = "PlayerA: " + String.valueOf(1000 - score1);
                    canvas.drawText(scoreA, screenW-150, 100, paint);
                }
            }

            if(X<(screenW - ballW)/2 && platesTouched>=1 && dX<0) {
                score2++;

                if (dX > 0) {
                    scoreB = "PlayerB: " + String.valueOf(1000 - score2);
                    canvas.drawText(scoreB, 100, 100, paint);
                }
            }

            if (score1>1000 ){
                dX=0; dY=0;
                Toast.makeText(MainActivity4.this, "Player A wins", Toast.LENGTH_SHORT).show();
            }
            else if (score2 >1000){
                dX=0; dY=0;
                Toast.makeText(MainActivity4.this, "Player B wins", Toast.LENGTH_SHORT).show();
            }
            var = dX;

            //Call the next frame.
            invalidate();
        }
    }


    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;

        MainActivity4.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord);
                Log.d("onLeScan run", "update UI");
                updateUi();
            }
        });
    }
}
