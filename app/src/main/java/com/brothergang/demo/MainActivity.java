package com.brothergang.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.brothergang.demo.eventbus.EventBusLite;

public class MainActivity extends AppCompatActivity {
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = (Button) findViewById(R.id.button);
        mTextView = (TextView)findViewById(R.id.textview);

        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        SecondActivity.class);
                startActivity(intent);
            }
        });
        EventBusLite.getInstance().register(this, "onTest");
    }

    public void onEvent(AnyEvent event) {
        String msg = "MainActivity onEvent：" + event.getMsg();
        mTextView.setText(msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public void onTest(AnyEvent event){
        String msg = "MainActivity onTest：" + event.getMsg();
        mTextView.setText(msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
//        EventBusLite.getDefault().unregister(this);//反注册EventBus
    }
}
