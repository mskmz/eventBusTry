package com.mskmz.eventbusannotation;


import com.mskmz.eventbusannotation.bean.ThreadMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在这里我们思考 的应该是接受方需要做的时间 也就是在方法上加入该标记 将会在事件响应的时候 通知该方法
 * 应该提供的功能 线程模式
 * 优先级
 * 粘性事件
 */
@Target(ElementType.METHOD) // 该注解作用在方法之上
@Retention(RetentionPolicy.CLASS) // 要在编译时进行一些预处理操作，注解会在class文件中存在 JVM中不存在
public @interface Subscribe {
  //线程模式
  ThreadMode threadMode() default ThreadMode.POSTING;

  //优先级
  int priority() default 0;

  //是否是粘性事件(进入延迟队列)
  boolean sticky() default false;
}
