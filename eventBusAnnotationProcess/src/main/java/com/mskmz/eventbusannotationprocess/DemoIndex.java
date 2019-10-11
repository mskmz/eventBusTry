package com.mskmz.eventbusannotationprocess;

import com.mskmz.eventbusannotation.SubscriberInfoIndex;
import com.mskmz.eventbusannotation.bean.EventBeans;
import com.mskmz.eventbusannotation.bean.SubscriberInfo;
import com.mskmz.eventbusannotation.bean.SubscriberMethod;

public class DemoIndex implements SubscriberInfoIndex {
  @Override
  public SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {
    switch (subscriberClass.getName()) {
      case "":
        return new EventBeans(subscriberClass, new SubscriberMethod[]{
            
        });
    }
    return null;
  }
}
