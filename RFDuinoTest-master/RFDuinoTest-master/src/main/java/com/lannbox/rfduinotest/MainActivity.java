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

import java.lang.reflect.Method;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
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
            int c=0;
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {

//                if(r!=0)
//                    if (rfduinoService.connect(bluetoothDevice.getAddress())) {
//                        upgradeState(STATE_CONNECTING);
//                    }

                if (r==0) {
                    while (c < 2) {
                        while (c == 0)
                            if (rfduinoService.connect(mickyMouse)) { //bluetoothDevice.getAddress()
                                upgradeState(STATE_CONNECTING);
                                Toast.makeText(MainActivity.this, "Micky Mouse is Connected", Toast.LENGTH_SHORT).show();
                                c++;
                                Log.d("Micky Mouse", "Micky Mouse is Connected");
//todo                                Log.d("Address", bluetoothDevice.getAddress());
//                                if(bluetoothDevice.createBond())
                                rfduinoService.disconnect();
                                Log.d("Micky Mouse", "Micky Mouse is disconnected");
                            }
                        while (c == 1) {

                            if (rfduinoService.connect(donaldDuck)) { //bluetoothDevice.getAddress()
                                upgradeState(STATE_CONNECTING);
                                Toast.makeText(MainActivity.this, "Donald Duck are Connected", Toast.LENGTH_SHORT).show();
                                c++;
                                Log.d("Donald Duck", "Donald Duck is Connected");
                                Log.d("Address", bluetoothDevice.getAddress());
                                bluetoothDevice.connectGatt(MainActivity.this, true, rfduinoService.mGattCallback );
                            }
                        }
                    }
//                    int i =0;
//                    if(bluetoothDevice.getAddress().equals(mickyMouse)) {
//                        rfduinoService.disconnect();
//                        do {
//
//                            rfduinoService.connect(donaldDuck);
//                            upgradeState(STATE_CONNECTING);
//                            Log.d("do while", "i=" + String.valueOf(i++));
//                        } while (bluetoothDevice.getAddress().equals(mickyMouse));
//                    }
                    r++;
                    Toast.makeText(MainActivity.this,"Both players are Connected", Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothAdapter.enable();
//        // Find Device
//        scanStatusText = (TextView) findViewById(R.id.scanStatus);

//        scanButton = (Button) findViewById(R.id.scan);
//        scanButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                scanStarted = true;
//                bluetoothAdapter.startLeScan(
//                        new UUID[]{ RFduinoService.UUID_SERVICE },
//                        MainActivity.this);
//            }
//        });

        scanStarted = true;
                bluetoothAdapter.startLeScan(
                        new UUID[]{ RFduinoService.UUID_SERVICE },
                        MainActivity.this);

        while(c==0) {
//            connectButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    v.setEnabled(false);
//                    connectionStatusText.setText("Connecting...");
//                    Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
//                    boolean out = bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
//                    Log.d("Connect", String.valueOf(out));
//                    if (out) {
//                        c = 1;
//                    }
//                }
//            });
            Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
            boolean out = bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
            Log.d("Connect", String.valueOf(out));
            if (out) {
                c = 1;
            }
        }

//        Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
//        boolean out=bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);

//        Log.d("Connect", String.valueOf(out));
        // Send
//        valueEdit = (EditData) findViewById(R.id.value);
//        valueEdit.setImeOptions(EditorInfo.IME_ACTION_SEND);
//        valueEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_SEND) {
//                    sendValueButton.callOnClick();
//                    return true;
//                }
//                return false;
//            }
//        });

//        sendZeroButton = (Button) findViewById(R.id.sendZero);
//        sendZeroButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                rfduinoService.send(new byte[]{0});
//            }
//        });

//        rfduinoService.send(new byte[]{0});

//        sendValueButton = (Button) findViewById(R.id.sendValue);
//        sendValueButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                rfduinoService.send(valueEdit.getData());
//            }
//        });

//        // Receive
//        clearButton = (Button) findViewById(R.id.clearData);
//        clearButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dataLayout.removeAllViews();
//            }
//        });

        dataLayout = (LinearLayout) findViewById(R.id.dataLayout);

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
        // Connect
//        boolean connected = false;
        String connectionText = "Disconnected";
        if (state == STATE_CONNECTING) {
            connectionText = "Connecting...";
        } else if (state == STATE_CONNECTED) {
//            connected = true;
            connectionText = "Connected";
        }
        Log.d("connection text", connectionText);

    }
    private void pairDevice(BluetoothDevice device) {
        try {
            Log.d("pairDevice()", "Start Pairing...");
            Method m = device.getClass()
                    .getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d("pairDevice()", "Pairing finished.");
        } catch (Exception e) {
            Log.e("pairDevice()", e.getMessage());
        }
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

            ourSong = MediaPlayer.create(MainActivity.this, R.raw.alarm_clock);
            ourSong2 = MediaPlayer.create(MainActivity.this, R.raw.buzzer2);
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

            //Draw background.
            canvas.drawBitmap(bgr, 0, 0, null);

            if(touchData!=null)
            if (touchData.equals("00")){
                platesTouched = 0;
            }else if(touchData.equals("01")){
                platesTouched = 1;
            }else if(touchData.equals("02")){
                platesTouched = 2;
            }else if(touchData.equals("03")){
                platesTouched = 3;
            }else if(touchData.equals("110")){
                platesTouched = -1;
                Toast.makeText(MainActivity.this, "Error connecting to Rfduino : No Data", Toast.LENGTH_SHORT).show();
//                rfduinoService.connect("EC:C7:D5:05:67:BF");
                Toast.makeText(MainActivity.this, "Attempting to reconnect",
                        Toast.LENGTH_SHORT).show();

//                rfduinoService.disconnect();
//
//                registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
//                registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
//                registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());
//
//                updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
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

            int flag = 0;
            int t;
//            Disconnect
            if (X == (screenW - ballW)){
                rfduinoService.disconnect();
                Log.d("Rebound","rebound disconnect");
                flag = -1;
            }else if(X == 0){
                rfduinoService.disconnect();
                Log.d("Rebound","rebound disconnect");
                flag = -2;
            }
            t = 0;
            while (flag == -1){
                var = 10;
                while(!rfduinoService.connect(mickyMouse)) {
                    Log.d("Micky connect", "not connected" + String.valueOf(t));
                    t++;
                }
                upgradeState(STATE_CONNECTING);
                if (t == 0){
                    flag = 1;
                    break;
                }

            }
//            rfduinoServiceConnection.onServiceConnected();

            t=0;
            while (flag == -2){
                var = -10;
                while(!rfduinoService.connect(donaldDuck)) {
                    Log.d("Donald connect", "not connected" + String.valueOf(t));
                    t++;
                }
                upgradeState(STATE_CONNECTING);

                if (t == 0){
//                    flag = 2;
                    break;
                }
            }





//            int t=0;
//            if (X>(screenW - ballW)/2 && dX<0) { //&& X<(screenW - ballW)/2
//              if(bluetoothDevice.getAddress().equals(donaldDuck)){
//                  rfduinoService.disconnect();
//                  Log.d("Phase I wall left","Donald disconnected");
//                  t=0;
//              }
//                if (rfduinoService.connect(mickyMouse))  {
////                    rfduinoService.connect(mickyMouse);
//                    Log.d("RDF Micky Mouse", "connected" + String.valueOf(t));
////                    t++;
//                }
//            }else if(X<(screenW - ballW)/2 && dX>0){
//                if(bluetoothDevice.getAddress().equals(mickyMouse)){
//                    rfduinoService.disconnect();
//                    Log.d("Phase I wall right","Micky disconnected");
//                    t=0;
//                }
//                while (t==0){
//                    rfduinoService.connect("EC:C7:D5:05:67:BF");
//                    Log.d("RDF Donald Duck", "connected" + String.valueOf(t));
//                    t++;
//                }
//            }

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
                Toast.makeText(MainActivity.this, "game over", Toast.LENGTH_SHORT).show();
                X = (screenW - ballW) / 2; //Centre ball into the centre of the screen.
                Y=screenH / 2 - (ballH / 2);

            }

            // The show must go on
            if(platesTouched == -1)
            if (X > (screenW - ballW)) {
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
            canvas.drawText(touchData, 50, 80, paint);
            canvas.drawText("X=" + String.valueOf(X), 100, 80, paint);
            canvas.drawText("Y=" + String.valueOf(Y), 150, 80, paint);
            String scoreA = "PlayerA \n" + String.valueOf(1000 - score1);
            String scoreB = "PlayerB \n" + String.valueOf(1000 - score2);
            canvas.drawText(scoreA, 50, 100, paint);
            canvas.drawText(scoreB, 50, 100, paint);

            if(X>(screenW - ballW)/2 && platesTouched>0 ){
                paint.setColor(Color.RED);
                canvas.drawRect(70, getTop(),getRight(),getBottom(),paint);
            }

            if(X>(screenW - ballW)/2 && platesTouched>0 && dX>0) {
                score1++;

                if (dX < 0) {
                    scoreA = "PlayerA \n" + String.valueOf(1000 - score1);
                    canvas.drawText(scoreA, 50, 100, paint);
                }
            }

            if(X<(screenW - ballW)/2 && platesTouched>=1 && dX<0) {
                score2++;
//                (X-screenW)/X*50

                if (dX > 0) {
                    scoreB = "PlayerB \n" + String.valueOf(1000 - score2);
                    canvas.drawText(scoreB, 50, 100, paint);
                }
            }

            if (score1>1000 ){
                dX=0; dY=0;
                Toast.makeText(MainActivity.this, "Player A wins", Toast.LENGTH_SHORT).show();
            }
            else if (score2 >1000){
                dX=0; dY=0;
                Toast.makeText(MainActivity.this, "Player B wins", Toast.LENGTH_SHORT).show();
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

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord);
                updateUi();
            }
        });
    }
}
