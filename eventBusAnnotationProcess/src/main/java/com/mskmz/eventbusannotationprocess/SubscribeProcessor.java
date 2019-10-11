package com.mskmz.eventbusannotationprocess;

import com.google.auto.service.AutoService;
import com.mskmz.eventbusannotation.Subscribe;
import com.mskmz.eventbusannotation.SubscriberInfoIndex;
import com.mskmz.eventbusannotation.bean.EventBeans;
import com.mskmz.eventbusannotation.bean.SubscriberInfo;
import com.mskmz.eventbusannotation.bean.SubscriberMethod;
import com.mskmz.eventbusannotation.bean.ThreadMode;
import com.mskmz.eventbusannotationprocess.utils.Constants;
import com.mskmz.eventbusannotationprocess.utils.EmptyUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({Constants.SUBSCRIBE_ANNOTATION_TYPES})
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
// 注解处理器接收的参数
@SupportedOptions({Constants.PACKAGE_NAME, Constants.CLASS_NAME})
public class SubscribeProcessor extends AbstractProcessor {
  //>>>>>>>>>>>>>>>DEBUG配置>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
  private static final String TAG = "SubscribeProcessor>>>";
  private static final boolean DEBUG = true;
  //<<<<<<<<<<<<<<<DEBUG配置<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
  // 操作Element工具类 (类、函数、属性都是Element)
  private Elements elementUtils;

  // type(类信息)工具类，包含用于操作TypeMirror的工具方法
  private Types typeUtils;

  // Messager用来报告错误，警告和其他提示信息
  private Messager messager;

  // 文件生成器 类/资源，Filter用来创建新的类文件，class文件以及辅助文件
  private Filer filer;

  // APT包名
  private String packageName;

  // APT类名
  private String className;

  /**
   * 特殊功能 需要根据类名做类型转换
   */
  //初始化上下文环境
  @Override
  public synchronized void init(ProcessingEnvironment processingEnvironment) {
    super.init(processingEnvironment);
    // 初始化
    elementUtils = processingEnvironment.getElementUtils();
    typeUtils = processingEnvironment.getTypeUtils();
    messager = processingEnvironment.getMessager();
    filer = processingEnvironment.getFiler();
    // 通过ProcessingEnvironment去获取对应的参数
    Map<String, String> options = processingEnvironment.getOptions();
    if (!EmptyUtils.isEmpty(options)) {
      packageName = options.get(Constants.PACKAGE_NAME);
      className = options.get(Constants.CLASS_NAME);
      messager.printMessage(Diagnostic.Kind.NOTE,
          "packageName >>> " + packageName + " / className >>> " + className);
    }

    // 必传参数判空（乱码问题：添加java控制台输出中文乱码）
    if (EmptyUtils.isEmpty(packageName) || EmptyUtils.isEmpty(className)) {
      messager.printMessage(Diagnostic.Kind.ERROR, "注解处理器需要的参数为空，请在对应build.gradle配置参数");
    }
    Log.messager = messager;
  }

  //在这里接受到方法的监听
  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    for (TypeElement element : set) {
      if (element.getSimpleName().toString()
          .equals(Subscribe.class.getSimpleName())) {
        processSubscribe(element, roundEnvironment);
      }
    }
    return false;
  }

  private void processSubscribe(TypeElement typeElement, RoundEnvironment roundEnvironment) {
    //创建Class
    TypeSpec.Builder classBuilder = TypeSpec
        .classBuilder(className)
        .addSuperinterface(ClassName.get(SubscriberInfoIndex.class))
        .addModifiers(Modifier.PUBLIC);
    //创建方法
    ParameterSpec pamaSubscriberClass = ParameterSpec.builder(
        ClassName.get(Class.class),
        Constants.GETSUBSCRIBERINFO_PARAMETER_NAME)
        .build();
    MethodSpec.Builder methodBuilder = MethodSpec
        .methodBuilder(Constants.GETSUBSCRIBERINFO_METHOD_NAME)
        .addAnnotation(Override.class)
        .addParameter(pamaSubscriberClass)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get(SubscriberInfo.class));
    //创建命令行
    //拼接
    Set<? extends Element> set = roundEnvironment.getElementsAnnotatedWith(typeElement);
    //switch (subscriberClass.getName()) {
    //      case "":
    //        return new EventBeans(subscriberClass, new SubscriberMethod[]{
    //
    //        });
    //    }

    Map<String, List<Element>> map = new HashMap<>();
    String className;
    List<Element> eList;
    for (Element e : set) {
      className = "";
      className = e.getEnclosingElement().toString();
      eList = null;
      eList = map.get(className);
      if (eList == null) {
        eList = new ArrayList<>();
      }
      eList.add(e);
      map.put(className, eList);
    }
    methodBuilder.beginControlFlow(
        "switch ($N.getName())",
        Constants.GETSUBSCRIBERINFO_PARAMETER_NAME
    );
    //      case "":
    //        return new EventBeans(subscriberClass, new SubscriberMethod[]{
    //
    //        });
    int index = 0;
    for (Map.Entry<String, List<Element>> entry : map.entrySet()) {
      methodBuilder.addCode("case $S:\r", entry.getKey());

      methodBuilder.addCode(
          "return new $T($N, new $T[]{\r",
          ClassName.get(EventBeans.class),
          Constants.GETSUBSCRIBERINFO_PARAMETER_NAME,
          ClassName.get(SubscriberMethod.class)
      );
      index = 0;
      ExecutableElement executableElement;
      TypeMirror typeMirror;
      Subscribe subscribe;
      for (Element e : entry.getValue()) {
        if (e instanceof ExecutableElement) {
          executableElement = (ExecutableElement) e;
        } else {
          executableElement = null;
        }
        if (executableElement == null) {
          continue;
        }
        try {
          typeMirror = executableElement.getParameters().get(0).asType();
        } catch (Exception ex) {
          continue;
        }

        subscribe = e.getAnnotation(Subscribe.class);
        if (index > 0) {
          methodBuilder.addCode(",");
        }
        //SubscriberMethod(
        //    Class subscriberClass,
        //    String methodName,
        //    Class<?> eventType,
        //    ThreadMode threadMode,
        //    int priority,
        //    boolean sticky
        //) {
        methodBuilder.addCode(
            "new  $T(" +
                "$T.class," +
                "$S," +
                "$T.class, " +
                "$T.$L, " +
                "$L, " +
                "$L" +
                ")",
            ClassName.get(SubscriberMethod.class),
            ClassName.get(e.getEnclosingElement().asType()),
            e.getSimpleName(),
            ClassName.get(typeMirror),
            ClassName.get(ThreadMode.class),
            subscribe.threadMode(),
            subscribe.priority(),
            subscribe.sticky()
        );
        index++;
      }
      methodBuilder.addStatement(
          "})"
      );

    }
    methodBuilder.endControlFlow();
    methodBuilder.addStatement("return null");
    //结束方法
    TypeSpec clazz = classBuilder.addMethod(methodBuilder.build()).build();
    try {
      JavaFile.builder(packageName, clazz).build().writeTo(filer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static class Log {
    static Messager messager;

    static void d(String str, String note) {
      messager.printMessage(Diagnostic.Kind.NOTE, TAG + note);
    }
  }
}
