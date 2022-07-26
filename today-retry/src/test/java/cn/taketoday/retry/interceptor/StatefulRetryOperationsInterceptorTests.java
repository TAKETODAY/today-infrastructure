/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.retry.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.retry.ExhaustedRetryException;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryOperations;
import cn.taketoday.retry.listener.RetryListenerSupport;
import cn.taketoday.retry.policy.AlwaysRetryPolicy;
import cn.taketoday.retry.policy.NeverRetryPolicy;
import cn.taketoday.retry.policy.SimpleRetryPolicy;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class StatefulRetryOperationsInterceptorTests {

  private StatefulRetryOperationsInterceptor interceptor;

  private final RetryTemplate retryTemplate = new RetryTemplate();

  private Service service;

  private Transformer transformer;

  private RetryContext context;

  private static int count;

  @BeforeEach
  public void setUp() {
    interceptor = new StatefulRetryOperationsInterceptor();
    retryTemplate.registerListener(new RetryListenerSupport() {
      @Override
      public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
              Throwable throwable) {
        StatefulRetryOperationsInterceptorTests.this.context = context;
      }
    });
    interceptor.setRetryOperations(retryTemplate);
    service = ProxyFactory.getProxy(Service.class, new SingletonTargetSource(new ServiceImpl()));
    transformer = ProxyFactory.getProxy(Transformer.class, new SingletonTargetSource(new TransformerImpl()));
    count = 0;
  }

  @Test
  public void testDefaultInterceptorSunnyDay() {
    ((Advised) service).addAdvice(interceptor);
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service("foo"))
            .withMessageStartingWith("Not enough calls");
  }

  @Test
  public void testDefaultInterceptorWithLabel() {
    interceptor.setLabel("FOO");
    ((Advised) service).addAdvice(interceptor);
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service("foo"))
            .withMessageStartingWith("Not enough calls");
    assertThat(count).isEqualTo(1);
    assertThat(context.getAttribute(RetryContext.NAME)).isEqualTo("FOO");
  }

  @Test
  public void testDefaultTransformerInterceptorSunnyDay() {
    ((Advised) transformer).addAdvice(interceptor);
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> transformer.transform("foo"))
            .withMessageStartingWith("Not enough calls");
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void testDefaultInterceptorAlwaysRetry() {
    retryTemplate.setRetryPolicy(new AlwaysRetryPolicy());
    interceptor.setRetryOperations(retryTemplate);
    ((Advised) service).addAdvice(interceptor);
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service("foo"))
            .withMessageStartingWith("Not enough calls");
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void testInterceptorChainWithRetry() throws Exception {
    ((Advised) service).addAdvice(interceptor);
    final List<String> list = new ArrayList<>();
    ((Advised) service).addAdvice((MethodInterceptor) invocation -> {
      list.add("chain");
      return invocation.proceed();
    });
    interceptor.setRetryOperations(retryTemplate);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service("foo"))
            .withMessageStartingWith("Not enough calls");
    assertThat(count).isEqualTo(1);
    service.service("foo");
    assertThat(count).isEqualTo(2);
    assertThat(list).hasSize(2);
  }

  @Test
  public void testTransformerWithSuccessfulRetry() throws Exception {
    ((Advised) transformer).addAdvice(interceptor);
    interceptor.setRetryOperations(retryTemplate);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> transformer.transform("foo"))
            .withMessageStartingWith("Not enough calls");
    assertThat(count).isEqualTo(1);
    Collection<String> result = transformer.transform("foo");
    assertThat(count).isEqualTo(2);
    assertThat(result).hasSize(1);
  }

  @Test
  public void testRetryExceptionAfterTooManyAttemptsWithNoRecovery() throws Exception {
    ((Advised) service).addAdvice(interceptor);
    interceptor.setRetryOperations(retryTemplate);
    retryTemplate.setRetryPolicy(new NeverRetryPolicy());
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service("foo"))
            .withMessageStartingWith("Not enough calls");
    assertThat(count).isEqualTo(1);
    assertThatExceptionOfType(ExhaustedRetryException.class).isThrownBy(() -> service.service("foo"))
            .withMessageStartingWith("Retry exhausted");
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void testRecoveryAfterTooManyAttempts() throws Exception {
    ((Advised) service).addAdvice(interceptor);
    interceptor.setRetryOperations(retryTemplate);
    retryTemplate.setRetryPolicy(new NeverRetryPolicy());
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service("foo"))
            .withMessageStartingWith("Not enough calls");
    assertThat(count).isEqualTo(1);
    interceptor.setRecoverer((data, cause) -> {
      count++;
      return null;
    });
    service.service("foo");
    assertThat(count).isEqualTo(2);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testKeyGeneratorReturningNull() throws Throwable {
    this.interceptor.setKeyGenerator(mock(MethodArgumentsKeyGenerator.class));
    this.interceptor.setLabel("foo");
    RetryOperations template = mock(RetryOperations.class);
    this.interceptor.setRetryOperations(template);
    MethodInvocation invocation = mock(MethodInvocation.class);
    when(invocation.getArguments()).thenReturn(new Object[] { new Object() });
    this.interceptor.invoke(invocation);
    ArgumentCaptor<DefaultRetryState> captor = ArgumentCaptor.forClass(DefaultRetryState.class);
    verify(template).execute(any(RetryCallback.class), eq(null), captor.capture());
    assertThat(captor.getValue().getKey()).isNull();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testKeyGeneratorAndRawKey() throws Throwable {
    this.interceptor.setKeyGenerator(item -> "bar");
    this.interceptor.setLabel("foo");
    this.interceptor.setUseRawKey(true);
    RetryOperations template = mock(RetryOperations.class);
    this.interceptor.setRetryOperations(template);
    MethodInvocation invocation = mock(MethodInvocation.class);
    when(invocation.getArguments()).thenReturn(new Object[] { new Object() });
    this.interceptor.invoke(invocation);
    ArgumentCaptor<DefaultRetryState> captor = ArgumentCaptor.forClass(DefaultRetryState.class);
    verify(template).execute(any(RetryCallback.class), eq(null), captor.capture());
    assertThat(captor.getValue().getKey()).isEqualTo("bar");
  }

  @Test
  public void testTransformerRecoveryAfterTooManyAttempts() throws Exception {
    ((Advised) transformer).addAdvice(interceptor);
    interceptor.setRetryOperations(retryTemplate);
    retryTemplate.setRetryPolicy(new NeverRetryPolicy());
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> transformer.transform("foo"))
            .withMessageStartingWith("Not enough calls");
    assertThat(count).isEqualTo(1);
    interceptor.setRecoverer((data, cause) -> {
      count++;
      return Collections.singleton((String) data[0]);
    });
    Collection<String> result = transformer.transform("foo");
    assertThat(count).isEqualTo(2);
    assertThat(result.size()).isEqualTo(1);
  }

  public static interface Service {

    void service(String in) throws Exception;

  }

  public static class ServiceImpl implements Service {

    @Override
    public void service(String in) throws Exception {
      count++;
      if (count < 2) {
        throw new Exception("Not enough calls: " + count);
      }
    }

  }

  public static interface Transformer {

    Collection<String> transform(String in) throws Exception;

  }

  public static class TransformerImpl implements Transformer {

    @Override
    public Collection<String> transform(String in) throws Exception {
      count++;
      if (count < 2) {
        throw new Exception("Not enough calls: " + count);
      }
      return Collections.singleton(in + ":" + count);
    }

  }

}
