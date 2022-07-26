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

package cn.taketoday.retry.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.policy.CircuitBreakerRetryPolicy;
import cn.taketoday.retry.support.RetrySynchronizationManager;
import cn.taketoday.retry.util.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class CircuitBreakerTests {

  @Test
  public void vanilla() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    Service service = context.getBean(Service.class);
    assertThat(AopUtils.isAopProxy(service)).isTrue();
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service());
    assertThat((Boolean) service.getContext().getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN)).isFalse();
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service());
    assertThat((Boolean) service.getContext().getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN)).isFalse();
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service());
    assertThat((Boolean) service.getContext().getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN)).isTrue();
    assertThat(service.getCount()).isEqualTo(3);
    assertThatExceptionOfType(Exception.class).isThrownBy(() -> service.service());
    // Not called again once circuit is open
    assertThat(service.getCount()).isEqualTo(3);
    service.expressionService();
    assertThat(service.getCount()).isEqualTo(4);
    service.expressionService2();
    assertThat(service.getCount()).isEqualTo(5);
    Advised advised = (Advised) service;
    Advisor advisor = advised.getAdvisors()[0];
    Map<?, ?> delegates = (Map<?, ?>) new DirectFieldAccessor(advisor).getPropertyValue("advice.delegates");
    assertThat(delegates).hasSize(1);
    Map<?, ?> methodMap = (Map<?, ?>) delegates.values().iterator().next();
    MethodInterceptor interceptor = (MethodInterceptor) methodMap
            .get(Service.class.getDeclaredMethod("expressionService"));
    DirectFieldAccessor accessor = new DirectFieldAccessor(interceptor);
    assertThat(accessor.getPropertyValue("retryOperations.retryPolicy.delegate.maxAttempts")).isEqualTo(8);
    assertThat(accessor.getPropertyValue("retryOperations.retryPolicy.openTimeout")).isEqualTo(19000L);
    assertThat(accessor.getPropertyValue("retryOperations.retryPolicy.resetTimeout")).isEqualTo(20000L);
    assertThat(accessor.getPropertyValue("retryOperations.retryPolicy.delegate.expression.expression"))
            .isEqualTo("#root instanceof RuntimeExpression");

    interceptor = (MethodInterceptor) methodMap.get(Service.class.getDeclaredMethod("expressionService2"));
    accessor = new DirectFieldAccessor(interceptor);
    assertThat(accessor.getPropertyValue("retryOperations.retryPolicy.delegate.maxAttempts")).isEqualTo(10);
    assertThat(accessor.getPropertyValue("retryOperations.retryPolicy.openTimeout")).isEqualTo(10000L);
    assertThat(accessor.getPropertyValue("retryOperations.retryPolicy.resetTimeout")).isEqualTo(20000L);
    context.close();
  }

  @Test
  void runtimeExpressions() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    Service service = context.getBean(Service.class);
    assertThat(AopUtils.isAopProxy(service)).isTrue();
    service.expressionService3();
    assertThat(service.getCount()).isEqualTo(1);
    Advised advised = (Advised) service;
    Advisor advisor = advised.getAdvisors()[0];
    Map<?, ?> delegates = (Map<?, ?>) new DirectFieldAccessor(advisor).getPropertyValue("advice.delegates");
    assertThat(delegates).hasSize(1);
    Map<?, ?> methodMap = (Map<?, ?>) delegates.values().iterator().next();
    MethodInterceptor interceptor = (MethodInterceptor) methodMap
            .get(Service.class.getDeclaredMethod("expressionService3"));
    Supplier<?> maxAttempts = TestUtils.getPropertyValue(interceptor,
            "retryOperations.retryPolicy.delegate.maxAttemptsSupplier", Supplier.class);
    assertThat(maxAttempts).isNotNull();
    assertThat(maxAttempts.get()).isEqualTo(10);
    CircuitBreakerRetryPolicy policy = TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy",
            CircuitBreakerRetryPolicy.class);
    Supplier openTO = TestUtils.getPropertyValue(policy, "openTimeoutSupplier", Supplier.class);
    assertThat(openTO).isNotNull();
    assertThat(openTO.get()).isEqualTo(10000L);
    Supplier resetTO = TestUtils.getPropertyValue(policy, "resetTimeoutSupplier", Supplier.class);
    assertThat(resetTO).isNotNull();
    assertThat(resetTO.get()).isEqualTo(20000L);
    RetryContext ctx = service.getContext();
    assertThat(TestUtils.getPropertyValue(ctx, "openWindow")).isEqualTo(10000L);
    assertThat(TestUtils.getPropertyValue(ctx, "timeout")).isEqualTo(20000L);
    context.close();
  }

  @Configuration
  @EnableRetry
  protected static class TestConfiguration {

    @Bean
    public Service service() {
      return new ServiceImpl();
    }

    @Bean
    Configs configs() {
      return new Configs();
    }

  }

  public static class Configs {

    public int maxAttempts = 10;

    public long openTimeout = 10000;

    public long resetTimeout = 20000;

  }

  interface Service {

    void service();

    void expressionService();

    void expressionService2();

    void expressionService3();

    int getCount();

    RetryContext getContext();

  }

  protected static class ServiceImpl implements Service {

    int count = 0;

    RetryContext context;

    @Override
    @CircuitBreaker(RuntimeException.class)
    public void service() {
      this.context = RetrySynchronizationManager.getContext();
      if (this.count++ < 5) {
        throw new RuntimeException("Planned");
      }
    }

    @Override
    @CircuitBreaker(maxAttemptsExpression = "#{2 * ${foo:4}}", openTimeoutExpression = "#{${bar:19}000}",
                    resetTimeoutExpression = "#{${baz:20}000}",
                    exceptionExpression = "#{#root instanceof RuntimeExpression}")
    public void expressionService() {
      this.count++;
    }

    @Override
    @CircuitBreaker(maxAttemptsExpression = "#{@configs.maxAttempts}",
                    openTimeoutExpression = "#{@configs.openTimeout}", resetTimeoutExpression = "#{@configs.resetTimeout}")
    public void expressionService2() {
      this.count++;
    }

    @Override
    @CircuitBreaker(maxAttemptsExpression = "@configs.maxAttempts", openTimeoutExpression = "@configs.openTimeout",
                    resetTimeoutExpression = "@configs.resetTimeout")
    public void expressionService3() {
      this.context = RetrySynchronizationManager.getContext();
      this.count++;
    }

    @Override
    public RetryContext getContext() {
      return this.context;
    }

    @Override
    public int getCount() {
      return this.count;
    }

  }

}
