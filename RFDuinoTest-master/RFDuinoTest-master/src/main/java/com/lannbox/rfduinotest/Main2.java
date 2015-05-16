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
import android.widget.Toast;

import java.util.Random;
import java.util.UUID;


public class Main2 extends Activity implements BluetoothAdapter.LeScanCallback {


    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    private int state;

    private boolean scanStarted;
    private boolean scanning;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;

    public static String mickyMouse = "EA:02:7F:9E:5F:7C";  //  "EA:02:7F:9E:5F:7C";
    public static String donaldDuck = "EC:C7:D5:05:67:BF";
    public static String touchData = "No Return Value";
    int c = 0, score1 = 100, score2 = 100, connect = 0;

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
            scanning = (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
            scanStarted &= scanning;
            updateUi();
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            int c = 0;
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                while (c < 2) {
                    while (c == 0)
                        if (rfduinoService.connect("EA:02:7F:9E:5F:7C")) { //bluetoothDevice.getAddress()
                            upgradeState(STATE_CONNECTING);
                            Toast.makeText(Main2.this, "Micky Mouse is Connected", Toast.LENGTH_SHORT).show();
                            c++;
                            Log.d("Micky Mouse", "Micky Mouse is Connected");
//                            Log.d("Address", bluetoothDevice.getAddress());
//TODO                            rfduinoService.disconnect();
                            Log.d("Micky Mouse", "Micky Mouse is disconnected");
                        }
                    while (c == 1)
                        if (rfduinoService.connect("EC:C7:D5:05:67:BF")) { //bluetoothDevice.getAddress()
                            upgradeState(STATE_CONNECTING);
                            Toast.makeText(Main2.this, "Donald Duck are Connected", Toast.LENGTH_SHORT).show();
                            c++;
                            Log.d("Donald Duck", "Donald Duck is Connected");
//                            Log.d("Address", bluetoothDevice.getAddress());
                        }
                }
                Toast.makeText(Main2.this, "Both players are Connected",
                        Toast.LENGTH_SHORT).show();
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
        //Remove title bar //Remove notification bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        this.setRequestedOrientation(WindowManager.LayoutParams.SCREEN_ORIENTATION_CHANGED);

        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();
        scanStarted = true;
        bluetoothAdapter.startLeScan(
                new UUID[]{RFduinoService.UUID_SERVICE},
                Main2.this);

        while (c == 0) {
            Intent rfduinoIntent = new Intent(Main2.this, RFduinoService.class);
            boolean out = bindService(rfduinoIntent,
                    rfduinoServiceConnection, BIND_AUTO_CREATE);
            if (out) {
                c = 1;
                Toast.makeText(Main2.this, "out = 1", Toast.LENGTH_SHORT).show();
            }
            Log.d("Connect", String.valueOf(out)); // ToDo
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

//ToDo    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        bluetoothAdapter.stopLeScan(this);
//        unregisterReceiver(scanModeReceiver);
//        unregisterReceiver(bluetoothStateReceiver);
//        unregisterReceiver(rfduinoReceiver);
//        bluetoothAdapter.disable();
//    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothAdapter.stopLeScan(this);
        unregisterReceiver(scanModeReceiver);
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(rfduinoReceiver);
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
        updateUi();
    }

    private void addData(byte[] data) {
        Log.d("Road", HexAsciiHelper.bytesToHex(data));

        touchData = HexAsciiHelper.bytesToHex(data);
        if (touchData.isEmpty()) {
            touchData = "No Return Value";
        }
        updateUi();
    }

    private void updateUi() {

        boolean on = state > STATE_BLUETOOTH_OFF;

        boolean connected = false;
        String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
            connectionText = "Connected";
            connect=1;
        }else if (state !=  STATE_CONNECTED) {
            connect=0;
        }
        Log.d("Connection Status", connectionText);

        if (bluetoothDevice != null && state == STATE_DISCONNECTED)  // Todo
            Log.d("ConnectionButtonStatus",
                    "bluetoothDevice != null && state == STATE_DISCONNECTED");
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
        float dY, dX, tX, tY;
        String scoreA, scoreB;

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
            ourSong = MediaPlayer.create(Main2.this, R.raw.alarm_clock);
            ourSong2 = MediaPlayer.create(Main2.this, R.raw.buzzer2);
            Random r1 = new Random();
            Random r2 = new Random();
            Intent intent = getIntent();
            int k;
            k = intent.getIntExtra("touchValue", 1);
            dX = r1.nextInt(1) + k;
            dY = r2.nextInt(1) + k;
            long startTime = System.currentTimeMillis();
            Paint text = new Paint();
            text.setColor(Color.RED);
            text.setTextSize(20);

        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            screenW = w;
            screenH = h;
            bgr = Bitmap.createScaledBitmap(bgr, w, h, true); //Resize background to fit the screen.
            X = (screenW - ballW) / 2; //Centre ball into the centre of the screen.
            Y = (screenH - ballH) / 2;
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            //Draw background.
            canvas.drawBitmap(bgr, 0, 0, null);

            // To make sure that the BTDevice is connected
// ToDO           while (bluetoothDevice == null) {
//                try {
//                    rfduinoService.connect(donaldDuck);
//                    Toast.makeText(Main2.this, "Bluetooth Device was NULL", Toast.LENGTH_SHORT).show();
//                } catch (Exception e) {
//                    Toast.makeText(Main2.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                } finally {
//                    rfduinoService.connect(mickyMouse);
//                }
//            }

            numberOfPlatesTouched();  // Assigns the touchData to platesTouched
            velocityFunction();
            music();
            rebound();
            activeSwapping();


            //Draw ball
            canvas.save(); //Save the position of the canvas.
            canvas.rotate(angle, X + (ballW / 2), Y + (ballH / 2)); //Rotate the canvas.
            canvas.drawBitmap(ball, X, Y, null); //Draw the ball on the rotated canvas.
            canvas.restore(); //Rotate the canvas back so that it looks like ball has rotated.

            Paint paint = new Paint(32);
            canvas.drawText(touchData, 50, 80, paint);
            canvas.drawText("X=" + String.valueOf(X), 100, 80, paint);
            canvas.drawText("Y=" + String.valueOf(Y), 150, 80, paint);
            scoreA = "PlayerA \n" + String.valueOf(1000 - score1);
            scoreB = "PlayerB \n" + String.valueOf(1000 - score2);
            canvas.drawText(scoreA, 50, 100, paint);
            canvas.drawText(scoreB, 50, 100, paint);

            Paint paint1 = new Paint();
            paint1.setColor(Color.BLACK);
//            paint1.setStrokeWidth(3);
//            canvas.drawRect(30, 30, 80, 80, paint1);
//            paint1.setStrokeWidth(0);

            if (X > (screenW - ballW) / 2 && platesTouched > 0 && dX > 0) {
                score1++;
                if (dX < 0) {
                    scoreA = "PlayerA \n" + String.valueOf(1000 - score1);
                    paint1.setColor(Color.RED);
                    canvas.drawRect(70, getTop(), getRight(), getBottom(), paint1 );

                }
            }

            if (X < (screenW - ballW) / 2 && platesTouched >= 1 && dX < 0) {
                score2++;
                if (dX > 0) {
                    scoreB = "PlayerB \n" + String.valueOf(1000 - score2);
                    paint1.setColor(Color.RED);
                    canvas.drawRect(getLeft(), getTop(), 70, getBottom(), paint1 );

                }
            }

            if (score1 > 1000) {
                tX=dX;
                tY=dY;
                dX = 0;
                dY = 0;
                Toast.makeText(Main2.this, "Player A wins", Toast.LENGTH_SHORT).show();
                timer(canvas);
            } else if (score2 > 1000) {
                dX = 0;
                dY = 0;
                Toast.makeText(Main2.this, "Player B wins", Toast.LENGTH_SHORT).show();
                timer(canvas);
            }

            //Call the next frame.
            invalidate();
        }

        public void timer(Canvas canvas) {
            long startTime = System.currentTimeMillis();
            Paint text = new Paint();
            text.setColor(Color.RED);
            text.setTextSize(20);
            long timeNow = System.currentTimeMillis();
            long timeToGo = 10 - (timeNow - startTime) / 1000;
            if (timeToGo >= 0) {
                canvas.drawText(Long.toString(timeToGo), 10, 25, text);
            }
            dX = tX;  // reviving X direction
            dY = tY;  // reviving Y direction
        }

        public void activeSwapping() {
            int t = 0;
            if(platesTouched != -1)
            if (X > (screenW - ballW) / 2 && dX < 0) { // TODO X>(screenW - ballW)/2
                if (bluetoothDevice.getAddress().equals(donaldDuck)) {
                    rfduinoService.disconnect();
                    int k=0;
                    while(!rfduinoService.connect(mickyMouse)) {
                        k++;
                        if(k>5) break;
                    }
// TODO                        rfduinoService.onBind(getIntent());
                    Log.d("Phase I wall left", "Donald disconnected");
                    t=0;
                }
                while (!bluetoothDevice.getAddress().equals(mickyMouse)) {
                    rfduinoService.connect(mickyMouse);
                    Log.d("RDF Micky Mouse", "Micky connected -- attacking left" + String.valueOf(t));
                    t++;
                    if (t==7){
                        Log.d("Left", "Process too heavy");
                        Toast.makeText(Main2.this, "Micky ,please check connection settings", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            } else if (X < (screenW - ballW) / 2 && dX > 0) {
                if (bluetoothDevice.getAddress().equals(mickyMouse)) {
// todo                   rfduinoService.disconnect();
                    rfduinoService.connect(donaldDuck);
                    Log.d("Phase I wall right", "Micky disconnected");
                    t=0;
                }
                while (!bluetoothDevice.getAddress().equals(donaldDuck)) {
                    rfduinoService.connect(donaldDuck);
                    Log.d("RDF Donald Duck", "Donald connected -- attacking right" + String.valueOf(t));
                    t++;
                    if (t==7){
                        Log.d("Right", "Process too heavy");
                        Toast.makeText(Main2.this, "Donald, please check connection settings", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        }

        public void velocityFunction() {
            //Compute roughly ball speed and location.
            Y += (int) dY; //Increase or decrease vertical position.
            X += (int) dX; //Increase or decrease vertical position.

            //Increase rotating angle.
            if (angle++ > 360)
                angle = 0;
        }

        public void rebound() {
            if (X >= (screenW - ballW) && platesTouched >= 1) {
                dX = (-1) * dX; //Reverse speed when bottom hit.
                if (ourSong.isPlaying())
                    ourSong.pause();
            } else if (X <= 0 && platesTouched >= 1) {
                dX = (-1) * dX;
                if (ourSong2.isPlaying())
                    ourSong2.pause();
            } else if (X <= 0 || X > (screenW - ballW)) {
                Toast.makeText(Main2.this, "game over", Toast.LENGTH_SHORT).show();
                X = (screenW - ballW) / 2; //Centre ball into the centre of the screen.
                Y = (screenH - ballH) / 2;
            }

            if (Y >= (screenH - ballH)) {
                dY = (-1) * dY; //Reverse speed when bottom hit.
            } else if (Y <= 0) {
                dY = (-1) * dY;
            }

            // The show must go on
//            if (platesTouched == -1 || bluetoothDevice != null)
            if (state != STATE_CONNECTED){
                if (X >= (screenW - ballW)) {
                    dX = (-1) * dX; //Reverse speed when bottom hit.
                    Toast.makeText(Main2.this, "Rebound Overrided: Please check your connection settings",
                            Toast.LENGTH_SHORT).show();
                    Log.d("Rebound", "Overrided");

                } else if (X <= 0) {
                    dX = (-1) * dX;
                    Toast.makeText(Main2.this, "Rebound Overrided: Please check your connection settings",
                            Toast.LENGTH_SHORT).show();
                    Log.d("Rebound", "Overrided");
                }
                Log.d("State override", "Override");
                Toast.makeText(Main2.this, "Please check your connection settings", Toast.LENGTH_SHORT).show();
            }
        }

        public void music() {
            if (X > (screenW) / 2 && dX > 0) {
                ourSong.start();
            }
            if (X < (screenW) / 2 & dX < 0) {
                ourSong2.start();
            }
        }

        public void numberOfPlatesTouched() {
            if(bluetoothDevice != null)
            if (touchData!=null)   // todo if(touchData!=null)
                if (touchData.equals("00")) {
                    platesTouched = 0;
                } else if (touchData.equals("01")) {
                    platesTouched = 1;
                } else if (touchData.equals("02")) {
                    platesTouched = 2;
                } else if (touchData.equals("03")) {
                    platesTouched = 3;
                } else if (touchData.equals("No Return Value")) {
                    platesTouched = -1;
                    Toast.makeText(Main2.this,
                            "Error connecting to RfDuino : No Data",
                            Toast.LENGTH_SHORT).show();
                    rfduinoService.connect(donaldDuck);  // todo see if its feasible
                    Toast.makeText(Main2.this, "Attempting to reconnect",
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;

        Main2.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Main2.this, BluetoothHelper.getDeviceInfoText(
                        bluetoothDevice, rssi, scanRecord), Toast.LENGTH_SHORT).show();
                updateUi();
            }
        });
    }
}
