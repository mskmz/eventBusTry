package com.mskmz.eventbus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mskmz.eventbusannotation.SubscriberInfoIndex;
import com.mskmz.eventbusannotation.bean.SubscriberInfo;
import com.mskmz.eventbusannotation.bean.SubscriberMethod;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//这里是对外暴露的工具类
//需要提供的功能
//post进行消息推送
//
public class EventBus {
  //>>>>>>>>>>>>>>>DEBUG配置>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
  private static final String TAG = "EventBus>>>";
  private static final boolean DEBUG = true;
  //<<<<<<<<<<<<<<<DEBUG配置<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

  // 索引接口
  private SubscriberInfoIndex mSubscriberInfoIndexes;
  // 订阅者类型集合，比如：订阅者MainActivity订阅了哪些EventBean，或者解除订阅的缓存。
  // key：订阅者MainActivity.class，value：EventBean集合
  private Map<Object, List<Class<?>>> mTypesBySubscriber;

  // EventBean缓存，key：UserInfo.class，value：订阅者（可以是多个Activity）中所有订阅的方法集合
  private Map<Class<?>, CopyOnWriteArrayList<Subscription>> mSubscriptionsByEventType;

  // 方法缓存：key：订阅者MainActivity.class，value：订阅方法集合
  private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

  // 粘性事件缓存，key：UserInfo.class，value：UserInfo
  private final Map<Class<?>, Object> mStickyEvents;

  // 发送（子线程） - 订阅（主线程）
  private Handler mHandler;
  // 发送（主线程） - 订阅（子线程）
  private ExecutorService mExecutorService;


  //>>>>>>>>>>>>>>>单例模式(静态内部类方式)>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
  private EventBus() {
    // 初始化缓存集合
    mTypesBySubscriber = new HashMap<>();
    mSubscriptionsByEventType = new WeakHashMap<>();
    mStickyEvents = new HashMap<>();
    // Handler高级用法：将handler放在主线程使用
    mHandler = new Handler(Looper.getMainLooper());
    // 创建一个子线程（缓存线程池）
    mExecutorService = Executors.newCachedThreadPool();
  }

  //实现唯一一份内部类
  public static EventBus getDefault() {
    return SingletonHolder.DEFAULT;
  }

  private static class SingletonHolder {
    private static volatile EventBus DEFAULT = new EventBus();
  }

  //<<<<<<<<<<<<<<<单例模式(静态内部类方式)<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
  // 添加索引（简化），接口 = 接口实现类，参考EventBusBuilder.java 136行
  public void addIndex(SubscriberInfoIndex index) {
    mSubscriberInfoIndexes = index;
  }

  public void register(Object object) {
    //尝试拿到方法
    //注册方法到缓存
    if (!METHOD_CACHE.containsKey(object.getClass())) {
      SubscriberInfo subscriberInfo = mSubscriberInfoIndexes.getSubscriberInfo(object.getClass());
      METHOD_CACHE.put(object.getClass()
          , Arrays.asList(subscriberInfo.getSubscriberMethods()));
    }
    List<SubscriberMethod> methodList = METHOD_CACHE.get(object.getClass());
    if (methodList == null || methodList.size() < 1) {
      return;
    }
    //注册方法
    List<Class<?>> classList = new ArrayList<>();

    for (SubscriberMethod method : methodList) {
      classList.add(method.getEventType());
      CopyOnWriteArrayList<Subscription> list = mSubscriptionsByEventType.get(method.getEventType());
      if (list == null) {
        list = new CopyOnWriteArrayList<>();
        mSubscriptionsByEventType.put(method.getEventType(), list);
      }
      Subscription subscription = new Subscription(new WeakReference<>(object), method);
      list.add(subscription);
//      Collections.sort(list, new Comparator<Subscription>() {
//        @Override
//        public int compare(Subscription o1, Subscription o2) {
//          return o1.subscriberMethod.getPriority() - o2.subscriberMethod.getPriority();
//        }
//      });
      //查看是否触发粘性事件
      postHistoryStickyEvents(object, method);
    }
    mTypesBySubscriber.put(object, classList);

  }

  private void logTypesBySubscriber() {
    if (DEBUG) Log.d(TAG, "logTypesBySubscriber: " + mTypesBySubscriber);
  }

  private void logSubscriptionsByEventType() {
    if (DEBUG) Log.d(TAG, "mSubscriptionsByEventType: " + mSubscriptionsByEventType);
  }

  private void postHistoryStickyEvents(Object activityObj, SubscriberMethod method) {
    if (!method.isSticky()
        || mStickyEvents == null
        || mStickyEvents.size() < 1
        || !mStickyEvents.containsKey(method.getEventType())
    ) {
      return;
    }
    Object object = mStickyEvents.get(method.getEventType());
    invokeMethod(method, activityObj, object);
  }

  public void unregister(Object object) {
    if (object == null) {
      return;
    }
    if (mTypesBySubscriber.get(object) != null) {
      for (Class<?> clazz : mTypesBySubscriber.get(object)) {
        if (mSubscriptionsByEventType.get(clazz) != null) {
          for (Subscription subscription : mSubscriptionsByEventType.get(clazz)) {
            if (subscription.subscriber.get().equals(object)) {
              mSubscriptionsByEventType.get(clazz).remove(subscription);
            }
          }
          List list = mSubscriptionsByEventType.get(clazz);
          if (list == null || list.size() < 1) {
            mSubscriptionsByEventType.remove(clazz);
          }
        }
      }
      mTypesBySubscriber.remove(object);
    }
  }

  //消息推送 普通消息推送
  public void post(Object event) {
    postSingleEventForEventType(event, event.getClass());
  }

  //粘性事件推送
  public void postSticky(Object event) {
    postSingleEventForEventType(event, event.getClass());//先将普通的方法先推送一遍
    mStickyEvents.put(event.getClass(), event);
  }

  //仅限延迟。。。
  public void postStickyOnly(Object event) {
    mStickyEvents.put(event.getClass(), event);
  }

  private void postSingleEventForEventType(Object event, Class<?> eventClass) {
    CopyOnWriteArrayList<Subscription> copyOnWriteArrayList
        = mSubscriptionsByEventType.get(eventClass);
    if (copyOnWriteArrayList == null
        || copyOnWriteArrayList.size() < 1) {
      return;
    }
    for (Subscription subscription : copyOnWriteArrayList) {
      invokeMethod(subscription, event);
    }
  }

  private void invokeMethod(Subscription subscription, Object event) {
    Object classObj = subscription.subscriber.get();
    if (classObj == null) {
      return;
    }
    invokeMethod(subscription.subscriberMethod, classObj, event);
  }

  private void invokeMethod(final SubscriberMethod subscriberMethod, final Object subscriberObj, final Object event) {
    if (subscriberMethod == null) {
      return;
    }
    switch (subscriberMethod.getThreadMode()) {
      case POSTING:
        invoke(subscriberMethod.getMethod(), subscriberObj, event);
        break;
      case MAIN:
        mHandler.post(
            new Runnable() {
              @Override
              public void run() {
                invoke(subscriberMethod.getMethod(), subscriberObj, event);
              }
            }
        );
        break;
      case ASYNC:
        mExecutorService.execute(new Runnable() {
          @Override
          public void run() {
            invoke(subscriberMethod.getMethod(), subscriberObj, event);
          }
        });
      default:
        break;
    }
  }


  private void invoke(Method method, Object obj, Object event) {
    try {
      method.invoke(obj, event);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }
}
