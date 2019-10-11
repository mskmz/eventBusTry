package com.mskmz.eventbus;

import androidx.annotation.Nullable;

import com.mskmz.eventbusannotation.bean.SubscriberMethod;

import java.lang.ref.WeakReference;

//事件方法的个体 有object对象
class Subscription {
  public Subscription() {
  }

  public Subscription(WeakReference<Object> subscriber, SubscriberMethod subscriberMethod) {
    this.subscriber = subscriber;
    this.subscriberMethod = subscriberMethod;
  }

  public WeakReference<Object> subscriber; // 订阅者Object
  public SubscriberMethod subscriberMethod; // 订阅的方法
}
