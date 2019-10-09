package com.mskmz.eventbusannotationprocess;

import com.google.auto.service.AutoService;
import com.mskmz.eventbusannotationprocess.utils.Constants;

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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({Constants.SUBSCRIBE_ANNOTATION_TYPES})
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
// 注解处理器接收的参数
@SupportedOptions({Constants.PACKAGE_NAME, Constants.CLASS_NAME})
public class SubscribeProcessor extends AbstractProcessor {
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
  }

  //在这里接受到方法的监听
  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    return false;
  }
}
