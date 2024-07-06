/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.validation.beanvalidation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.ObjectProvider;
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
import cn.taketoday.validation.method.MethodValidationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 */
public class MethodValidationProxyTests {

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  @SuppressWarnings("unchecked")
  void testMethodValidationInterceptor(boolean adaptViolations) {
    MyValidBean bean = new MyValidBean();
    ProxyFactory factory = new ProxyFactory(bean);
    factory.addAdvice(adaptViolations ?
            new MethodValidationInterceptor(() -> Validation.buildDefaultValidatorFactory().getValidator(), true) :
            new MethodValidationInterceptor());
    factory.addAdvisor(new AsyncAnnotationAdvisor());
    doTestProxyValidation((MyValidInterface<String>) factory.getProxy(),
            (adaptViolations ? MethodValidationException.class : ConstraintViolationException.class));
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  @SuppressWarnings("unchecked")
  void testMethodValidationPostProcessor(boolean adaptViolations) {
    StaticApplicationContext context = new StaticApplicationContext();
    context.registerBean(MethodValidationPostProcessor.class, adaptViolations ?
            () -> {
              MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
              postProcessor.setAdaptConstraintViolations(true);
              return postProcessor;
            } :
            MethodValidationPostProcessor::new);
    PropertyValues pvs = new PropertyValues();
    pvs.add("beforeExistingAdvisors", false);
    context.registerSingleton("aapp", AsyncAnnotationBeanPostProcessor.class, pvs);
    context.registerSingleton("bean", MyValidBean.class);
    context.refresh();
    doTestProxyValidation(context.getBean("bean", MyValidInterface.class),
            adaptViolations ? MethodValidationException.class : ConstraintViolationException.class);
    context.close();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMethodValidationPostProcessorForInterfaceOnlyProxy() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(MethodValidationPostProcessor.class);
    context.registerBean(MyValidInterface.class, () ->
            ProxyFactory.getProxy(MyValidInterface.class, new MyValidClientInterfaceMethodInterceptor()));
    context.refresh();
    doTestProxyValidation(context.getBean(MyValidInterface.class), ConstraintViolationException.class);
    context.close();
  }

  @SuppressWarnings("DataFlowIssue")
  private void doTestProxyValidation(MyValidInterface<String> proxy, Class<? extends Exception> expectedExceptionClass) {
    assertThat(proxy.myValidMethod("value", 5)).isNotNull();
    assertThatExceptionOfType(expectedExceptionClass).isThrownBy(() -> proxy.myValidMethod("value", 15));
    assertThatExceptionOfType(expectedExceptionClass).isThrownBy(() -> proxy.myValidMethod(null, 5));
    assertThatExceptionOfType(expectedExceptionClass).isThrownBy(() -> proxy.myValidMethod("value", 0));
    proxy.myValidAsyncMethod("value", 5);
    assertThatExceptionOfType(expectedExceptionClass).isThrownBy(() -> proxy.myValidAsyncMethod("value", 15));
    assertThatExceptionOfType(expectedExceptionClass).isThrownBy(() -> proxy.myValidAsyncMethod(null, 5));
    assertThat(proxy.myGenericMethod("myValue")).isEqualTo("myValue");
    assertThatExceptionOfType(expectedExceptionClass).isThrownBy(() -> proxy.myGenericMethod(null));
  }

  @Test
  void testLazyValidatorForMethodValidation() {
    doTestLazyValidatorForMethodValidation(LazyMethodValidationConfig.class);
  }

  @Test
  void testLazyValidatorForMethodValidationWithProxyTargetClass() {
    doTestLazyValidatorForMethodValidation(LazyMethodValidationConfigWithProxyTargetClass.class);
  }

  @Test
  void testLazyValidatorForMethodValidationWithValidatorProvider() {
    doTestLazyValidatorForMethodValidation(LazyMethodValidationConfigWithValidatorProvider.class);
  }

  private void doTestLazyValidatorForMethodValidation(Class<?> configClass) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(configClass, CustomValidatorBean.class, MyValidBean.class, MyValidFactoryBean.class);
    context.getBeanFactory().getBeanDefinition("customValidatorBean").setLazyInit(true);
    context.refresh();

    assertThat(context.getBeanFactory().containsSingleton("customValidatorBean")).isFalse();
    context.getBeansOfType(MyValidInterface.class).values().forEach(bean -> bean.myValidMethod("value", 5));
    assertThat(context.getBeanFactory().containsSingleton("customValidatorBean")).isTrue();

    context.close();
  }

  @MyStereotype
  public static class MyValidBean implements MyValidInterface<String> {

    @SuppressWarnings("DataFlowIssue")
    @NotNull
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

    @SuppressWarnings("DataFlowIssue")
    @NotNull
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

    @NotNull
    Object myValidMethod(@NotNull(groups = MyGroup.class) String arg1, @Max(10) int arg2);

    @MyValid
    @Async
    void myValidAsyncMethod(@NotNull(groups = OtherGroup.class) String arg1, @Max(10) int arg2);

    T myGenericMethod(@NotNull T value);
  }

  static class MyValidClientInterfaceMethodInterceptor implements MethodInterceptor {

    private final MyValidBean myValidBean = new MyValidBean();

    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) {
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

  @Configuration
  public static class LazyMethodValidationConfigWithValidatorProvider {

    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor(ObjectProvider<Validator> validator) {
      MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
      postProcessor.setValidatorProvider(validator);
      return postProcessor;
    }
  }

}
