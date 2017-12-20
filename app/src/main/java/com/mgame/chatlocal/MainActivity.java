package com.mgame.chatlocal;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.mgame.chatlocal.client.ClientActivity;
import com.mgame.chatlocal.server.ServerActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnServer;
    private Button btnClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnServer = findViewById(R.id.btnServer);
        btnClient = findViewById(R.id.btnClient);

        btnServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerActivity.start(MainActivity.this);
            }
        });
        btnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClientActivity.start(MainActivity.this);
            }
        });
    }
}
