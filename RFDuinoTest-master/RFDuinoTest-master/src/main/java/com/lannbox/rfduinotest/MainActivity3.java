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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.UUID;

public class MainActivity3 extends Activity implements BluetoothAdapter.LeScanCallback {
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

    //    private Button enableBluetoothButton;
//    private TextView scanStatusText;
//    private Button scanButton;
    private TextView deviceInfoText;
    private TextView connectionStatusText;
    private Button connectButton;
    //    private EditData valueEdit;
//    private Button sendZeroButton;
//    private Button sendValueButton;
//    private Button clearButton;
    private LinearLayout dataLayout;

    public static  String mickyMouse = "EA:02:7F:9E:5F:7C";  //  "EA:02:7F:9E:5F:7C";
    public static  String donaldDuck = "EC:C7:D5:05:67:BF";
    public static String touchData="110";
    int c =0, score1 = 100,score2 = 100, r=0;
    float var = 1;

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
            int c=0;
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {

                if (r==0) {
                    while (c < 2) {
                        while (c == 0)
                            if (rfduinoService.connect("EA:02:7F:9E:5F:7C")) { //bluetoothDevice.getAddress()
                                upgradeState(STATE_CONNECTING);
                                Toast.makeText(MainActivity3.this, "Micky Mouse is Connected", Toast.LENGTH_SHORT).show();
                                c++;
                                Log.d("Micky Mouse", "Micky Mouse is Connected");
                                Log.d("Address", bluetoothDevice.getAddress());
//                        if(bluetoothDevice.createBond())
                                rfduinoService.disconnect();
                                Log.d("Micky Mouse", "Micky Mouse is disconnected");
                            }
                        while (c == 1)
                            if (rfduinoService.connect("EC:C7:D5:05:67:BF")) { //bluetoothDevice.getAddress()
                                upgradeState(STATE_CONNECTING);
                                Toast.makeText(MainActivity3.this, "Donald Duck are Connected", Toast.LENGTH_SHORT).show();
                                c++;
                                Log.d("Donald Duck", "Donald Duck is Connected");
                                Log.d("Address", bluetoothDevice.getAddress());
                            }
                    }
                    r++;

                    Toast.makeText(MainActivity3.this, "Both players are Connected", Toast.LENGTH_SHORT).show();
                }else if(var > 0){
                    if (rfduinoService.connect("EC:C7:D5:05:67:BF")) {
                        upgradeState(STATE_CONNECTING);
                    }
                }
                else if (var<0)
                    if (rfduinoService.connect("EA:02:7F:9E:5F:7C")) {
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
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //set content view AFTER ABOVE sequence (to avoid crash)
//        this.setContentView(R.layout.your_layout_name_here);
        setContentView(R.layout.activity_main_activity3);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Bluetooth
//        enableBluetoothButton = (Button) findViewById(R.id.enableBluetooth);
//        enableBluetoothButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                enableBluetoothButton.setEnabled(false);
//                enableBluetoothButton.setText(
//                        bluetoothAdapter.enable() ? "Enabling bluetooth..." : "Enable failed!");
//            }
//        });
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
                MainActivity3.this);

        // Device Info
        deviceInfoText = (TextView) findViewById(R.id.deviceInfo);

        // Connect Device
        connectionStatusText = (TextView) findViewById(R.id.connectionStatus);


        while(c==0) {
            connectButton = (Button) findViewById(R.id.connect);
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
            Intent rfduinoIntent = new Intent(MainActivity3.this, RFduinoService.class);
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
        View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2,
                dataLayout, false);

        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        text1.setText(HexAsciiHelper.bytesToHex(data));

        Log.d("Road", HexAsciiHelper.bytesToHex(data));

        touchData = HexAsciiHelper.bytesToHex(data);
        if (touchData == null){
            touchData = "10";
        }

        String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
        if (ascii != null) {
            TextView text2 = (TextView) view.findViewById(android.R.id.text2);
            text2.setText(ascii);
        }

        updateUi();

        dataLayout.addView(
                view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }


    private void updateUi() {
        // Enable Bluetooth
        boolean on = state > STATE_BLUETOOTH_OFF;
//        enableBluetoothButton.setEnabled(!on);
//        enableBluetoothButton.setText(on ? "Bluetooth enabled" : "Enable Bluetooth");
//        scanButton.setEnabled(on);

//        // Scan
//        if (scanStarted && scanning) {
//            scanStatusText.setText("Scanning...");
//            scanButton.setText("Stop Scan");
//            scanButton.setEnabled(true);
//        } else if (scanStarted) {
//            scanStatusText.setText("Scan started...");
//            scanButton.setEnabled(false);
//        } else {
//            scanStatusText.setText("");
//            scanButton.setText("Scan");
//            scanButton.setEnabled(true);
//        }

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

        connectionStatusText.setText(connectionText);
        connectButton.setEnabled(bluetoothDevice != null && state == STATE_DISCONNECTED);

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

            ourSong = MediaPlayer.create(MainActivity3.this, R.raw.alarm_clock);
            ourSong2 = MediaPlayer.create(MainActivity3.this, R.raw.buzzer2);
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
                    Toast.makeText(MainActivity3.this, "Error connecting to Rfduino : No Data", Toast.LENGTH_SHORT).show();
//                rfduinoService.connect("EC:C7:D5:05:67:BF");
                    Toast.makeText(MainActivity3.this, "Attempting to reconnect",
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

            int t=0;
            if (X>(screenW - ballW)/2 && dX<0) { //&& X<(screenW - ballW)/2
                if(bluetoothDevice.getAddress().equals(donaldDuck)){
                    rfduinoService.disconnect();
                    Log.d("Phase I wall left","Donald disconnected");
                    t=0;
                }
                if (rfduinoService.connect(mickyMouse))  {
//                    rfduinoService.connect(mickyMouse);
                    Log.d("RDF Micky Mouse", "connected" + String.valueOf(t));
//                    t++;
                }
            }else if(X<(screenW - ballW)/2 && dX>0){
                if(bluetoothDevice.getAddress().equals(mickyMouse)){
                    rfduinoService.disconnect();
                    Log.d("Phase I wall right","Micky disconnected");
                    t=0;
                }
                while (t==0){
                    rfduinoService.connect("EC:C7:D5:05:67:BF");
                    Log.d("RDF Donald Duck", "connected" + String.valueOf(t));
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
                Toast.makeText(MainActivity3.this, "game over", Toast.LENGTH_SHORT).show();
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
            canvas.drawText("X=" + String.valueOf(X), 100, 80, paint);
            canvas.drawText("Y=" + String.valueOf(Y), 150, 80, paint);
            String scoreA = "PlayerA \n" + String.valueOf(1000 - score1);
            String scoreB = "PlayerB \n" + String.valueOf(1000 - score2);
            canvas.drawText(scoreA, 50, 100, paint);
            canvas.drawText(scoreB, 50, 100, paint);


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
                Toast.makeText(MainActivity3.this, "Player A wins", Toast.LENGTH_SHORT).show();
            }
            else if (score2 >1000){
                dX=0; dY=0;
                Toast.makeText(MainActivity3.this, "Player B wins", Toast.LENGTH_SHORT).show();
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

        MainActivity3.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceInfoText.setText(
                        BluetoothHelper.getDeviceInfoText(bluetoothDevice, rssi, scanRecord));
                updateUi();
            }
        });
    }
}
