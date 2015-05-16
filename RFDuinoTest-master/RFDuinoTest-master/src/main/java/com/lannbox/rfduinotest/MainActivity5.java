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
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.Random;
import java.util.UUID;

public class MainActivity5 extends Activity implements BluetoothAdapter.LeScanCallback {
    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    public final static String PREFS  = "touchValue";

    private int state;
    int start = 0;


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private RFduinoService rfduinoService;

    int currentPlayer = 0;
    public static String[] players = {"EA:02:7F:9E:5F:7C", "EC:C7:D5:05:67:BF"};
    int oneTimeToken = 0;
    public static int[] score = {1000, 1000};  // (scoreA, scoreB)
    public static int reboundChecker = 0;
    public static int resetCounter = 0;
    public static Thread sounds, soundStart;

    private boolean scanStarted;

    public static  String mickyMouse = "EA:02:7F:9E:5F:7C";  //  "EA:02:7F:9E:5F:7C";
    public static  String donaldDuck = "EC:C7:D5:05:67:BF";
    public static String touchData="110";
    int c =0, score1 = 100,score2 = 100, r=0;
    float var =1;

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
            updateUi();
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize())
                    if (rfduinoService.connect(players[currentPlayer])) {
                        upgradeState(STATE_CONNECTING);
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

        setContentView(R.layout.main_activity5);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();
        scanStarted = true;
        bluetoothAdapter.startLeScan(
                new UUID[]{ RFduinoService.UUID_SERVICE },
                MainActivity5.this);

        while(c==0) {
            Intent rfduinoIntent = new Intent(MainActivity5.this, RFduinoService.class);
            boolean out = bindService(rfduinoIntent,
                    rfduinoServiceConnection, BIND_AUTO_CREATE);
            Log.d("OnCreate Connect", "(bind service value)" + String.valueOf(out));
            if (out) {
                c = 1;
                break;
            }
        }

        // Begin
        Button begin = (Button)findViewById(R.id.begin);
        begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start = 1;
            }
        });
        if(start == 1)
            setContentView(new BallBounce(this));
//        setContentView(new BallBounce(this));



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
        updateUi();
    }

    private void addData(byte[] data) {
        Log.d("Road", HexAsciiHelper.bytesToHex(data));
        touchData = HexAsciiHelper.bytesToHex(data);
        if (touchData == null){
            touchData = "10";
        }
        updateUi();
    }


    private void updateUi() {
        String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
            connectionText = "Connected";
        }
        Log.d("connection text", connectionText);
    }


    class BallBounce extends View {
        int screenW, screenH, X, Y, initialY, ballW, ballH, angle, screenLength, screenBreadth;
        float dY = 1, dX = 1;
        int platesTouched = 0;
        Bitmap ball, bgr;
        MediaPlayer ourSongA, ourSongB, touchA,touchB;
        MediaPlayer[] songs, touch;
        String[] player = {"Player A", "Player B"};

        // Global Coordinates
        int[] Xcoord;  // Xa, Xb

        public BallBounce(Context context) {
            super(context);
            ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball); //load a ball image
            bgr = BitmapFactory.decodeResource(getResources(), R.drawable.sky1); //load a background

            ballW = ball.getWidth();
            ballH = ball.getHeight();
// songs

            ourSongA = MediaPlayer.create(MainActivity5.this, R.raw.alarm_clock);
            ourSongB = MediaPlayer.create(MainActivity5.this, R.raw.buzzer2);
            touchA = MediaPlayer.create(MainActivity5.this, R.raw.dading);
            touchB = MediaPlayer.create(MainActivity5.this, R.raw.robotblip);

            songs = new MediaPlayer[]{ourSongA, ourSongB};
            touch = new MediaPlayer[]{touchA, touchB};

            initialY = 0;


            Random r1= new Random();
            Random r2= new Random();
//            SharedPreferences touchValue = PREFS;
            Intent intent =getIntent();

            int k = intent.getIntExtra("touchValue",1);

            dX=r1.nextInt(1)+1;
            dY=r2.nextInt(1)+1;

            sounds = new Thread(new Runnable() {
                @Override
                public void run() {
                    allSounds();
                }
            });
            sounds.start();

            soundStart = new Thread(new Runnable() {
                @Override
                public void run() {
                    if(X>=(screenLength)/2 && dX>0){
                        songs[currentPlayer].start();
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                songs[currentPlayer].pause();
                            }
                        }, 2000);
                    }

                    if(X<(screenLength)/2 & dX<0)
                    {songs[currentPlayer].start();}
                }
            });
            soundStart.start();
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            screenW = w;
            screenH = h;
            bgr = Bitmap.createScaledBitmap(bgr, w, h, true); //Resize background to fit the screen.
            X = (screenW - ballW)/2; //Centre ball into the centre of the screen.
            Y = (screenH - ballH)/2;
        }

        @Override
        public void onDraw(final Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawBitmap(bgr, 0, 0, null);

            screenW = canvas.getWidth();
            screenH = canvas.getHeight();
            screenLength = screenW - ballW;
            screenBreadth = screenH - ballH;

            Xcoord = new int[]{X, screenLength - X};

            Button btn = new Button(getApplicationContext());

            plateAssign();

            Y += (int) dY;
            X += (int) dX;

//            soundStart.run();
            coreEngine(oneTimeToken);
//            sounds.run();

            reboundInY();

            if(Xcoord[currentPlayer] == screenLength)
            if(platesTouched!=0){
                reboundInX();
            }
            else{
                X = screenLength/2;
                Y = screenBreadth/2;
            }

            if(reboundChecker == 1){
                if(songs[currentPlayer].isPlaying())
                    songs[currentPlayer].pause();
                reboundChecker =0;
            }

            // The show must go on
            if ((Xcoord[currentPlayer] >= screenLength) && (platesTouched == -1 || platesTouched == -2)) {
                dX = (-1) * dX; //Reverse speed when bottom hit.
                Log.d("Rebound","rebound auto");
            }

            final Paint paint = new Paint(32);
            final Rect[] rect = {new Rect((int)(screenW*.95), getTop(),getRight(),getBottom()),
                    new Rect(getLeft(), getTop(),(int)(screenW*.05),getBottom())};

            warning();
            graphicsAndMovement(canvas, paint);
            scoreUpdate(canvas, paint);

//            Thread soundTouch = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    drawOnCanvasRedBarsAndSound(canvas, paint, rect);
//                }
//            });
//            soundTouch.start();

            var = dX;

            //Call the next frame.
            invalidate();
        }

        private void warning() {
            if(state < STATE_CONNECTING && (X == (screenW*0.6) || X == (screenW*0.4))){
                platesTouched = -1;
                Toast.makeText(MainActivity5.this,
                        "Please Restart " + player[currentPlayer], Toast.LENGTH_SHORT).show();
            }
        }

        private void drawOnCanvasRedBarsAndSound(Canvas canvas, Paint paint, Rect[] rect) {
            if(platesTouched>0){
                paint.setColor(Color.RED);
                canvas.drawRect(rect[currentPlayer],paint);
                touch[currentPlayer].start();
            }
        }

        private void graphicsAndMovement(Canvas canvas, Paint paint) {
            //Increase rotating angle.
            if (angle++ > 360)
                angle = 0;
            //Draw ball
            canvas.save(); //Save the position of the canvas.
            canvas.rotate(angle, X + (ballW / 2), Y + (ballH / 2)); //Rotate the canvas.
            canvas.drawBitmap(ball, X, Y, null); //Draw the ball on the rotated canvas.
            canvas.restore(); //Rotate the canvas back so that it looks like ball has rotated.

            canvas.drawText("Touch Data" + touchData, 100, 80, paint);
            canvas.drawText("Plates Touched" + String.valueOf(platesTouched), 200, 120, paint);
            canvas.drawText("X=" + String.valueOf(X), 200, 60, paint);
            canvas.drawText("Y=" + String.valueOf(Y), 250, 60, paint);

            paint.setColor(Color.BLACK);
            canvas.drawLine(screenW/2, 0,screenW/2, screenH, paint);
        }

        private void allSounds() {

        }

        private void scoreUpdate(Canvas canvas, Paint paint){
            // Score
            if(Xcoord[currentPlayer]>(screenLength)/2 && platesTouched>0) {
                score[currentPlayer]++;
            }

            // Winning Rule
            if (score[currentPlayer]>1000){
                dX=0; dY=0;
                Toast.makeText(MainActivity5.this, player[1-currentPlayer] + " wins", Toast.LENGTH_SHORT).show();
                resetCounter = 1;
            }

            // Score Draw
            String scoreA = player[0] + " \n" + String.valueOf(1000 - score[0]);
            String scoreB = player[1] + "\n" + String.valueOf(1000 - score[1]);
            canvas.drawText(scoreA, screenW-150, 100, paint);
            canvas.drawText(scoreB, 100, 100, paint);
        }


        private void plateAssign() {
            if(touchData!=null) {
                if (touchData.equals("00")) {
                    platesTouched = 0;
                } else if (touchData.equals("01")) {
                    platesTouched = 1;
                } else if (touchData.equals("02")) {
                    platesTouched = 2;
                } else if (touchData.equals("03")) {
                    platesTouched = 3;
                } else if (touchData.equals("110")) {
                    platesTouched = -1;
                    Toast.makeText(MainActivity5.this, "Error connecting to Rfduino : No Data", Toast.LENGTH_SHORT).show();
                }
            }else {   // Means touchData == null
                platesTouched = -2;
                Toast.makeText(MainActivity5.this, "Error receiving Data",
                        Toast.LENGTH_SHORT).show();
            }
        }

        private void coreEngine(int token) {
            Intent rfduinoIntent = new Intent(MainActivity5.this, RFduinoService.class);
            bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);

            if (state >= STATE_CONNECTING)
            if (token ==0 && (Xcoord[currentPlayer] >= screenLength) ){  // && !bluetoothDevice.getAddress().isEmpty()
                // first time so don't disconnect
                oneTimeToken++;
            }

            // Disconnect at screen end in X
            if ((Xcoord[currentPlayer] >= screenLength)) { // X == screenLength || X == 0
                if (platesTouched > 0) {
                    if (token != 0) {
                        reboundInX();
                        rfduinoService.disconnect();
                        currentPlayer = 1 - currentPlayer;
                        rfduinoService.connect(players[currentPlayer]);
                    }
                }
                else if (platesTouched == 0)
                    resetOnFail();
                else if (platesTouched < 0 && state < STATE_CONNECTING) {
                    reboundInX();
                    Log.d("Rebound in X", "Plates Touched < 0");
                    rfduinoService.disconnect();
                    rfduinoService.connect(players[currentPlayer]);
                }
            }

            if(!bluetoothAdapter.getAddress().equals(players[currentPlayer])){
                rfduinoService.disconnect();
                rfduinoService.connect(players[currentPlayer]);
            }

            Toast.makeText(MainActivity5.this,
                    "Please Restart " + player[currentPlayer], Toast.LENGTH_SHORT).show();


            final Toast toast = Toast.makeText(MainActivity5.this,
                    "This message will disappear in 1 second", Toast.LENGTH_SHORT);
            toast.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    toast.cancel();
                }
            }, 500);

            if(touchData == null) {
                platesTouched = -1;
                if(var>0)
                    rfduinoService.connect(donaldDuck);
                else if(var<0)
                    rfduinoService.connect(mickyMouse);
            }
        }

        private int reboundInX() {
            dX = - dX;
            reboundChecker = 1;
            return reboundChecker;
        }

        private void reboundInY(){
            if (Y >= (screenBreadth)) {
                dY = (-1) * dY; //Reverse speed when bottom hit.
            }else if(Y <= 0){
                dY = (-1) * dY;
            }
        }

        private void resetOnFail() {
            if (bluetoothDevice.getAddress().equals(players[currentPlayer]))  // as there can be connectivity issues also
                if (X <= 0 || X >= screenLength){
                    X = screenLength/2;
                    Y = screenBreadth/2;
                    Toast.makeText(getApplicationContext() ,"Game Reset",Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;

        MainActivity5.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord);
                Log.d("onLeScan run", "update UI");
                updateUi();
            }
        });
    }
}
