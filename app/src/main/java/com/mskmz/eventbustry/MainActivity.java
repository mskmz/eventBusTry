package com.mskmz.eventbustry;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.mskmz.eventbus.EventBus;
import com.mskmz.eventbus.apt.EventBusIndex;
import com.mskmz.eventbusannotation.Subscribe;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  //>>>>>>>>>>>>>>>DEBUG配置>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
  private static final String TAG = "MainActivity>>>";
  private static final boolean DEBUG = true;
  //<<<<<<<<<<<<<<<DEBUG配置<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    EventBus.getDefault().addIndex(new EventBusIndex());
    EventBus.getDefault().register(this);
    TextView tv = findViewById(R.id.tv_name);
    tv.setText(TAG);
    findViewById(R.id.btn_next).setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    EventBus.getDefault().postSticky("发送事件");
    Intent intent = new Intent(this, Main2Activity.class);
    startActivity(intent);
  }

  @Subscribe(sticky = true)
  public void onMessageEvent(String str) {
    if (DEBUG) Log.d(TAG, "recrvice: "+str);
  }
}
