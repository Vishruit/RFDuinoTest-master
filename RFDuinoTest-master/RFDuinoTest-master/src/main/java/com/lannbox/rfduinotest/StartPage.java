package com.lannbox.rfduinotest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class StartPage extends Activity {

    Button easy,medium, hard,instructions, exit;
    Intent openActivityMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_page);
        easy = (Button) findViewById(R.id.buttonEasy);
        medium = (Button) findViewById(R.id.buttonMedium);
        hard = (Button) findViewById(R.id.buttonHard);
        instructions = (Button) findViewById(R.id.buttonInstructions);
        exit = (Button) findViewById(R.id.buttonExit);

        easy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityMain = new Intent(view.getContext(), Final.class);
                openActivityMain.putExtra("touch speed", 1);
                openActivityMain.putExtra("Mode", "Easy");
                startActivity(openActivityMain);
            }
        });
        medium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityMain = new Intent(view.getContext(), Final.class);
                openActivityMain.putExtra("touch speed", 2);
                openActivityMain.putExtra("Mode", "Medium");
                startActivity(openActivityMain);
            }
        });
        hard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityMain = new Intent(view.getContext(), Final.class);
                openActivityMain.putExtra("touch speed", 4);
                openActivityMain.putExtra("Mode", "Difficult");
                startActivity(openActivityMain);
            }
        });
        instructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityMain = new Intent(view.getContext(), Instructions.class);
                openActivityMain.putExtra("touch speed", 1);
                openActivityMain.putExtra("Mode", "Easy");
                startActivity(openActivityMain);
            }
        });
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


    }

}
