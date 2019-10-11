package com.mskmz.eventbustry;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mskmz.eventbus.EventBus;
import com.mskmz.eventbusannotation.Subscribe;

public class Main3Activity extends AppCompatActivity implements View.OnClickListener {
  //>>>>>>>>>>>>>>>DEBUG配置>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
  private static final String TAG = "Main3Activity>>>";
  private static final boolean DEBUG = true;
  //<<<<<<<<<<<<<<<DEBUG配置<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    EventBus.getDefault().register(this);
    TextView tv = findViewById(R.id.tv_name);
    tv.setText(TAG);
    findViewById(R.id.btn_next).setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    EventBus.getDefault().postSticky("发送事件3");
  }

  @Override
  protected void onDestroy() {
    EventBus.getDefault().unregister(this);
    super.onDestroy();
  }

  @Subscribe(sticky = true)
  public void onMessageEvent(String str) {
    if (DEBUG) Log.d(TAG, "recrvice: "+str);
  }
}
