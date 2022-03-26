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
import org.junit.Test;

import java.util.Map;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class CircuitBreakerTests {

  @Test
  public void vanilla() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    Service service = context.getBean(Service.class);
    assertTrue(AopUtils.isAopProxy(service));
    try {
      service.service();
      fail("Expected exception");
    }
    catch (Exception e) {
    }
    assertFalse((Boolean) service.getContext().getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN));
    try {
      service.service();
      fail("Expected exception");
    }
    catch (Exception e) {
    }
    assertFalse((Boolean) service.getContext().getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN));
    try {
      service.service();
      fail("Expected exception");
    }
    catch (Exception e) {
    }
    assertTrue((Boolean) service.getContext().getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN));
    assertEquals(3, service.getCount());
    try {
      service.service();
      fail("Expected exception");
    }
    catch (Exception e) {
    }
    // Not called again once circuit is open
    assertEquals(3, service.getCount());
    service.expressionService();
    assertEquals(4, service.getCount());
    Advised advised = (Advised) service;
    Advisor advisor = advised.getAdvisors()[0];
    Map<?, ?> delegates = (Map<?, ?>) new DirectFieldAccessor(advisor).getPropertyValue("advice.delegates");
    assertTrue(delegates.size() == 1);
    Map<?, ?> methodMap = (Map<?, ?>) delegates.values().iterator().next();
    MethodInterceptor interceptor = (MethodInterceptor) methodMap
            .get(Service.class.getDeclaredMethod("expressionService"));
    DirectFieldAccessor accessor = new DirectFieldAccessor(interceptor);
    assertEquals(8, accessor.getPropertyValue("retryOperations.retryPolicy.delegate.maxAttempts"));
    assertEquals(19000L, accessor.getPropertyValue("retryOperations.retryPolicy.openTimeout"));
    assertEquals(20000L, accessor.getPropertyValue("retryOperations.retryPolicy.resetTimeout"));
    assertEquals("#root instanceof RuntimeExpression",
            accessor.getPropertyValue("retryOperations.retryPolicy.delegate.expression.expression"));
    context.close();
  }

  @Configuration
  @EnableRetry
  protected static class TestConfiguration {

    @Bean
    public Service service() {
      return new ServiceImpl();
    }

  }

  interface Service {

    void service();

    void expressionService();

    int getCount();

    RetryContext getContext();

  }

  protected static class ServiceImpl implements Service {

    private int count = 0;

    private RetryContext context;

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
    public RetryContext getContext() {
      return this.context;
    }

    @Override
    public int getCount() {
      return this.count;
    }

  }

}
