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
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Random;
import java.util.UUID;

public class Main4 extends Activity implements BluetoothAdapter.LeScanCallback {
    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;
    private final static String TAG = Main4.class.getSimpleName();

    private int state;

    private boolean scanStarted;
    private boolean scanning;

    public int var = 0;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;

    public static  String mickyMouse = "EA:02:7F:9E:5F:7C";  //  "EA:02:7F:9E:5F:7C";
    public static  String donaldDuck = "EC:C7:D5:05:67:BF";
    public static String touchData="10";
    int c =0, score1 = 100,score2 = 100;

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
            Log.d(TAG, "bluetooth State receiver");
        }
    };

    private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "a1");
            scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            Log.d(TAG, "a2");
            scanStarted &= scanning;
            Log.d(TAG, "a3");
            updateUi();
            Log.d(TAG, "a4");
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            int c=0;
            Log.d(TAG, "1");
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            Log.d(TAG, "2");
            if (rfduinoService.initialize()) {
                Log.d(TAG, "3");
                while(c<2){
                    Log.d(TAG, "4");
                    while(c==0)
                        if (rfduinoService.connect("EA:02:7F:9E:5F:7C")) { //bluetoothDevice.getAddress()
                            Log.d(TAG, "5");
                            upgradeState(STATE_CONNECTING);
//                            Toast.makeText(Main4.this,"Micky Mouse is Connected", Toast.LENGTH_SHORT).show();
                            c++;
                            Intent rfduinoIntent = new Intent(Main4.this, RFduinoService.class);
                            Log.d(TAG, "6");
                            bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
                            Log.d(TAG, "7");
                            int t =0,k =0;
                            while(t==0) {
                                k++;
                                if (state == STATE_CONNECTED) {
                                    Log.d("Address", bluetoothDevice.getAddress());
                                    Log.d(TAG, "Micky Mouse is Connected");
                                    t++;
                                }else if(k>5) {
                                    Log.d(TAG, "Micky Mouse is still not Connected");
                                    break;
                                }
                            }
                            rfduinoService.disconnect();
                            Log.d("Micky Mouse", "Micky Mouse is disconnected");
                            Log.d(TAG, "8");
                        }
                    while(c==1)
                        if (rfduinoService.connect("EC:C7:D5:05:67:BF")) { //bluetoothDevice.getAddress()
                            Log.d(TAG, "9");
                            upgradeState(STATE_CONNECTING);
                            Toast.makeText(Main4.this,"Donald Duck are Connected", Toast.LENGTH_SHORT).show();
                            c++;
                            Log.d(TAG, "10");
                            Intent rfduinoIntent = new Intent(Main4.this, RFduinoService.class);
                            Log.d(TAG, "11");
                            bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
                            Log.d(TAG, "12");
                            int t =0, k=0;
                            while(t==0) {
                                k++;
                                if (state == STATE_CONNECTED) {
                                    Log.d("Donald Duck", "Donald Duck is Connected");
                                    Log.d("Address", bluetoothDevice.getAddress());
                                    Log.d(TAG, "13");
                                    t++;
                                } else if (k > 5) {
                                    Log.d(TAG, "Donald Duck is still not Connected" + String.valueOf(k));
                                    break;
                                }
                            }
                        }
                    Log.d(TAG, "13");
                }
                Log.d(TAG, "14");
                Toast.makeText(Main4.this,"Both players are Connected", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "15");
            rfduinoService = null;
            Log.d(TAG, "16");
            downgradeState(STATE_DISCONNECTED);
            Log.d(TAG, "17");
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
        setContentView(R.layout.activity_main4);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();
        scanStarted = true;
        bluetoothAdapter.startLeScan(
                new UUID[]{ RFduinoService.UUID_SERVICE },
                Main4.this);

        while(c==0) {
            Intent rfduinoIntent = new Intent(Main4.this, RFduinoService.class);
            boolean out = bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
            Log.d("Connect bindService", String.valueOf(out));
            if (out)
                c = 1;
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
        updateUi();
    }

    private void addData(byte[] data) {
        Log.d("Road", HexAsciiHelper.bytesToHex(data));
        touchData = HexAsciiHelper.bytesToHex(data);
        if (touchData == null){
            touchData = "10";
        }

        String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
        if (ascii != null) {
//            TextView text2 = (TextView) findViewById(android.R.id.text2);
//            text2.setText(ascii);
            touchData = ascii;
        }
        updateUi();
    }


    private void updateUi() {
        // Connect
        boolean connected = false;
        String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
            connected = true;
            connectionText = "Connected";
        }
        Log.d("connection text", connectionText);

//        connectButton.setEnabled(bluetoothDevice != null && state == STATE_DISCONNECTED);

        // Send
//        sendZeroButton.setEnabled(connected);
//        sendValueButton.setEnabled(connected);


    }
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
        MediaPlayer ourSong, ourSong2;

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
//            while(k==0)
            k = intent.getIntExtra("touchValue",1);
            dX=r1.nextInt(1)+k;
            dY=r2.nextInt(1)+k;
//                platesTouched = 0;

            ourSong = MediaPlayer.create(Main4.this, R.raw.alarm_clock);
            ourSong2 = MediaPlayer.create(Main4.this, R.raw.buzzer2);
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            screenW = w;
            screenH = h;
            bgr = Bitmap.createScaledBitmap(bgr, w, h, true); //Resize background to fit the screen.
            X = (screenW / 2) - (ballW / 2); //Centre ball into the centre of the screen.
//            Y = initialY;
            Y=screenH / 2 - (ballH / 2);
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawBitmap(bgr, 0, 0, null);
            if(bluetoothDevice!=null)
                if (touchData.equals("00")){
                    platesTouched = 0;
                }else if(touchData.equals("01")){
                    platesTouched = 1;
                }else if(touchData.equals("02")){
                    platesTouched = 2;
                }else if(touchData.equals("03")){
                    platesTouched = 3;
                }else if(touchData.equals("10")){
                    platesTouched = -1;
                    Toast.makeText(Main4.this, "Error connecting to Rfduino : No Data", Toast.LENGTH_SHORT).show();
                rfduinoService.connect("EC:C7:D5:05:67:BF");
                    Toast.makeText(Main4.this, "Attempting to reconnect",
                            Toast.LENGTH_SHORT).show();

//                rfduinoService.disconnect();
                }

            //Compute roughly ball speed and location.
            Y += (int) dY; //Increase or decrease vertical position.
            X += (int) dX; //Increase or decrease vertical position.

            if(Y>(screenH)/2 && dY>0)
            {
                ourSong.start();
            }
            if(Y<(screenH)/2 & dY<0)
            {
                ourSong2.start();
            }

            int t=0;
            if (X>(screenW - ballW)/2 && dX<0) { //&& X<(screenW - ballW)/2
                if(bluetoothDevice.getAddress().equals(donaldDuck)){
//TODO                    rfduinoService.disconnect();
                    Log.d("Phase I wall left","Donald disconnected");
                    t=0;
                }
                while (t==0)  {
                    rfduinoService.connect(mickyMouse);
                    Log.d("RDF Micky Mouse", "connected" + String.valueOf(t));
                    t++;
                }
            }
            if(X<(screenW - ballW)/2 && dX>0){
                if(bluetoothDevice.getAddress().equals(mickyMouse)){
                    rfduinoService.disconnect();
                    Log.d("Phase I wall right","Micky disconnected");
                    t=0;
                }
                while (t==0){
                    boolean out = rfduinoService.connect("EC:C7:D5:05:67:BF");  //TAG
                    Log.d("RDF Donald Duck", "connected" + String.valueOf(t));

                    if(out)
                    t++;
                }
            }

            if (Y >= (screenH - ballH)) {
                dY = (-1) * dY; //Reverse speed when bottom hit.
                if(ourSong.isPlaying())
                    ourSong.pause();
//                ourSong.seekTo(0);

            }else if(Y <= 0){
                dY = (-1) * dY;
                if (ourSong2.isPlaying())
                    ourSong2.pause();
            }
            if (X > (screenW - ballW) && platesTouched ==1) {
                dX = (-1) * dX; //Reverse speed when bottom hit.
            }else if(X <= 0 && platesTouched ==2){
                dX = (-1) * dX;
            }else if (X <= 0 || X > (screenW - ballW)){
                Toast.makeText(Main4.this, "game over", Toast.LENGTH_SHORT).show();
                X = (screenW - ballW) / 2; //Centre ball into the centre of the screen.
                Y=screenH / 2 - (ballH / 2);

            }

            // The show must go on
            if(platesTouched == -1)
                if (X > (screenW - ballW)) {
                    dX = (-1) * dX; //Reverse speed when bottom hit.
                }else if(X <= 0){
                    dX = (-1) * dX;
                }

            //Increase rotating angle.
            if (angle++ > 360)
                angle = 0;

            //Draw ball
            canvas.save(); //Save the position of the canvas.
            canvas.rotate(angle, X + (ballW / 2), Y + (ballH / 2)); //Rotate the canvas.
            canvas.drawBitmap(ball, X, Y, null); //Draw the ball on the rotated canvas.
            canvas.restore(); //Rotate the canvas back so that it looks like ball has rotated.
//            if(touchData!=null)
//            canvas.drawText("Player",10,10,null);
            Paint paint = new Paint(32);
            canvas.drawText(touchData, 50, 80, paint);
            canvas.drawRect(getLeft(),getTop(),20, getBottom(),paint);


            if(X>(screenW - ballW)/2 && platesTouched>0 && dX>0) {
                score1++;

                if (dX < 0) {
                    String scoreA = "PlayerA \n" + String.valueOf(1000 - score1);
                    canvas.drawText(scoreA, 50, 100, paint);
                }
            }

            if(X<(screenW - ballW)/2 && platesTouched>=1 && dX<0) {
                score2++;
//                (X-screenW)/X*50

                if (dX > 0) {
                    String scoreB = "PlayerB \n" + String.valueOf(1000 - score2);
                    canvas.drawText(scoreB, 50, 100, paint);
                }
            }

            if (score1>1000 ){
                dX=0; dY=0;
                Toast.makeText(Main4.this, "Player A wins", Toast.LENGTH_SHORT).show();
            }
            else if (score2 >1000){
                dX=0; dY=0;
                Toast.makeText(Main4.this, "Player B wins", Toast.LENGTH_SHORT).show();
            }

            //Call the next frame.
            invalidate();
        }
    }


    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;

        Main4.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord);
                updateUi();
            }
        });
    }
}
