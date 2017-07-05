package com.brothergang.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.brothergang.demo.eventbus.EventBusLite;

public class SecondActivity extends AppCompatActivity {
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        Button btn = (Button) findViewById(R.id.button);
        mTextView = (TextView)findViewById(R.id.textview);

        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EventBusLite.getInstance().post(
                        new AnyEvent("Button Clicked!"));
            }
        });
        EventBusLite.getInstance().register(this, "onEvent");
    }

    public void onEvent(AnyEvent event) {
        String msg = "SecondActivity onEvent：" + event.getMsg();
        mTextView.setText(msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
//        EventBus.getDefault().unregister(this);//反注册EventBus
    }
}
