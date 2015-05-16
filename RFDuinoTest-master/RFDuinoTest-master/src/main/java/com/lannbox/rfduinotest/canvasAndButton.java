package com.lannbox.rfduinotest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class canvasAndButton extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas_and_button);

        RelativeLayout canvas;
        ImageView imageView;
        Bitmap mainImage;

        canvas = (RelativeLayout) findViewById(R.id.canvasLayout1);
        canvas.setDrawingCacheBackgroundColor(Color.YELLOW);
        imageView = new ImageView(getApplicationContext());
        mainImage = BitmapFactory.decodeResource(getResources(), R.drawable.sky1);
        imageView.setImageBitmap( mainImage );
        canvas.addView( imageView );

        Button btn = (Button) findViewById(R.id.btn);

        ((cBaseApplication)this.getApplicationContext()).rfduinoService.connect(MainActivity5.players[0]);
        int t = ((cBaseApplication)this.getApplicationContext()).currentPlayer;

        Log.d("current player", String.valueOf(t));

    }
}
