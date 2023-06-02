/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.validation.beanvalidation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.annotation.Async;
import cn.taketoday.scheduling.annotation.AsyncAnnotationAdvisor;
import cn.taketoday.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.validation.annotation.Validated;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
public class MethodValidationTests {

  @Test
  @SuppressWarnings("unchecked")
  public void testMethodValidationInterceptor() {
    MyValidBean bean = new MyValidBean();
    ProxyFactory proxyFactory = new ProxyFactory(bean);
    proxyFactory.addAdvice(new MethodValidationInterceptor());
    proxyFactory.addAdvisor(new AsyncAnnotationAdvisor());
    doTestProxyValidation((MyValidInterface<String>) proxyFactory.getProxy());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMethodValidationPostProcessor() {
    StaticApplicationContext ac = new StaticApplicationContext();
    ac.registerSingleton("mvpp", MethodValidationPostProcessor.class);
    PropertyValues pvs = new PropertyValues();
    pvs.add("beforeExistingAdvisors", false);
    ac.registerSingleton("aapp", AsyncAnnotationBeanPostProcessor.class, pvs);
    ac.registerSingleton("bean", MyValidBean.class);
    ac.refresh();
    doTestProxyValidation(ac.getBean("bean", MyValidInterface.class));
    ac.close();
  }

  @Test // gh-29782
  @SuppressWarnings("unchecked")
  public void testMethodValidationPostProcessorForInterfaceOnlyProxy() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(MethodValidationPostProcessor.class);
    context.registerBean(MyValidInterface.class, () ->
            ProxyFactory.getProxy(MyValidInterface.class, new MyValidClientInterfaceMethodInterceptor()));
    context.refresh();
    doTestProxyValidation(context.getBean(MyValidInterface.class));
    context.close();
  }

  private void doTestProxyValidation(MyValidInterface<String> proxy) {
    assertThat(proxy.myValidMethod("value", 5)).isNotNull();
    assertThatExceptionOfType(ValidationException.class).isThrownBy(() ->
            proxy.myValidMethod("value", 15));
    assertThatExceptionOfType(ValidationException.class).isThrownBy(() ->
            proxy.myValidMethod(null, 5));
    assertThatExceptionOfType(ValidationException.class).isThrownBy(() ->
            proxy.myValidMethod("value", 0));
    proxy.myValidAsyncMethod("value", 5);
    assertThatExceptionOfType(ValidationException.class).isThrownBy(() ->
            proxy.myValidAsyncMethod("value", 15));
    assertThatExceptionOfType(ValidationException.class).isThrownBy(() ->
            proxy.myValidAsyncMethod(null, 5));
    assertThat(proxy.myGenericMethod("myValue")).isEqualTo("myValue");
    assertThatExceptionOfType(ValidationException.class).isThrownBy(() ->
            proxy.myGenericMethod(null));
  }

  @Test
  public void testLazyValidatorForMethodValidation() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            LazyMethodValidationConfig.class, CustomValidatorBean.class,
            MyValidBean.class, MyValidFactoryBean.class);
    ctx.getBeansOfType(MyValidInterface.class).values().forEach(bean -> bean.myValidMethod("value", 5));
  }

  @Test
  public void testLazyValidatorForMethodValidationWithProxyTargetClass() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            LazyMethodValidationConfigWithProxyTargetClass.class, CustomValidatorBean.class,
            MyValidBean.class, MyValidFactoryBean.class);
    ctx.getBeansOfType(MyValidInterface.class).values().forEach(bean -> bean.myValidMethod("value", 5));
  }

  @MyStereotype
  public static class MyValidBean implements MyValidInterface<String> {

    @Override
    public Object myValidMethod(String arg1, int arg2) {
      return (arg2 == 0 ? null : "value");
    }

    @Override
    public void myValidAsyncMethod(String arg1, int arg2) {
    }

    @Override
    public String myGenericMethod(String value) {
      return value;
    }
  }

  @MyStereotype
  public static class MyValidFactoryBean implements FactoryBean<String>, MyValidInterface<String> {

    @Override
    public String getObject() {
      return null;
    }

    @Override
    public Class<?> getObjectType() {
      return String.class;
    }

    @Override
    public Object myValidMethod(String arg1, int arg2) {
      return (arg2 == 0 ? null : "value");
    }

    @Override
    public void myValidAsyncMethod(String arg1, int arg2) {
    }

    @Override
    public String myGenericMethod(String value) {
      return value;
    }
  }

  @MyStereotype
  public interface MyValidInterface<T> {

    @NotNull Object myValidMethod(@NotNull(groups = MyGroup.class) String arg1, @Max(10) int arg2);

    @MyValid
    @Async
    void myValidAsyncMethod(@NotNull(groups = OtherGroup.class) String arg1, @Max(10) int arg2);

    T myGenericMethod(@NotNull T value);
  }

  static class MyValidClientInterfaceMethodInterceptor implements MethodInterceptor {

    private final MyValidBean myValidBean = new MyValidBean();

    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      Method method;
      try {
        method = ReflectionUtils.getMethod(MyValidBean.class, invocation.getMethod().getName(), (Class<?>[]) null);
      }
      catch (IllegalStateException ex) {
        method = BridgeMethodResolver.findBridgedMethod(
                ReflectionUtils.getMostSpecificMethod(invocation.getMethod(), MyValidBean.class));
      }
      return ReflectionUtils.invokeMethod(method, this.myValidBean, invocation.getArguments());
    }
  }

  public interface MyGroup {
  }

  public interface OtherGroup {
  }

  @Validated({ MyGroup.class, Default.class })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MyStereotype {
  }

  @Validated({ OtherGroup.class, Default.class })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MyValid {
  }

  @Configuration
  public static class LazyMethodValidationConfig {

    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor(@Lazy Validator validator) {
      MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
      postProcessor.setValidator(validator);
      return postProcessor;
    }
  }

  @Configuration
  public static class LazyMethodValidationConfigWithProxyTargetClass {

    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor(@Lazy Validator validator) {
      MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
      postProcessor.setValidator(validator);
      postProcessor.setProxyTargetClass(true);
      return postProcessor;
    }
  }

}
