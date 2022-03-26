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
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.retry.RetryOperations;
import cn.taketoday.retry.backoff.FixedBackOffPolicy;
import cn.taketoday.retry.policy.SimpleRetryPolicy;
import cn.taketoday.retry.support.RetryTemplate;
import cn.taketoday.retry.util.test.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.0
 */
public class RetryInterceptorBuilderTests {

  @Test
  public void testBasic() {
    StatefulRetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateful().build();
    assertEquals(3, TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy.maxAttempts"));
  }

  @Test
  public void testWithCustomRetryTemplate() {
    RetryOperations retryOperations = new RetryTemplate();
    StatefulRetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateful()
            .retryOperations(retryOperations).build();
    assertEquals(3, TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy.maxAttempts"));
    assertSame(retryOperations, TestUtils.getPropertyValue(interceptor, "retryOperations"));
  }

  @Test
  public void testWithMoreAttempts() {
    StatefulRetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateful().maxAttempts(5).build();
    assertEquals(5, TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy.maxAttempts"));
  }

  @Test
  public void testWithCustomizedBackOffMoreAttempts() {
    StatefulRetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateful().maxAttempts(5)
            .backOffOptions(1, 2, 10).build();

    assertEquals(5, TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy.maxAttempts"));
    assertEquals(1L, TestUtils.getPropertyValue(interceptor, "retryOperations.backOffPolicy.initialInterval"));
    assertEquals(2.0, TestUtils.getPropertyValue(interceptor, "retryOperations.backOffPolicy.multiplier"));
    assertEquals(10L, TestUtils.getPropertyValue(interceptor, "retryOperations.backOffPolicy.maxInterval"));
  }

  @Test
  public void testWithCustomBackOffPolicy() {
    StatefulRetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateful().maxAttempts(5)
            .backOffPolicy(new FixedBackOffPolicy()).build();

    assertEquals(5, TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy.maxAttempts"));
    assertEquals(1000L, TestUtils.getPropertyValue(interceptor, "retryOperations.backOffPolicy.backOffPeriod"));
  }

  @Test
  public void testWithCustomNewMessageIdentifier() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    StatefulRetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateful().maxAttempts(5)
            .newMethodArgumentsIdentifier(new NewMethodArgumentsIdentifier() {

              @Override
              public boolean isNew(Object[] args) {
                latch.countDown();
                return false;
              }
            }).backOffPolicy(new FixedBackOffPolicy()).build();

    assertEquals(5, TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy.maxAttempts"));
    assertEquals(1000L, TestUtils.getPropertyValue(interceptor, "retryOperations.backOffPolicy.backOffPeriod"));
    final AtomicInteger count = new AtomicInteger();
    Foo delegate = createDelegate(interceptor, count);
    Object message = "";
    try {
      delegate.onMessage("", message);
    }
    catch (RuntimeException e) {
      assertEquals("foo", e.getMessage());
    }
    assertEquals(1, count.get());
    assertTrue(latch.await(0, TimeUnit.SECONDS));
  }

  @Test
  public void testWitCustomRetryPolicyTraverseCause() {
    StatefulRetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateful()
            .retryPolicy(new SimpleRetryPolicy(15,
                    Collections.<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true), true))
            .build();
    assertEquals(15, TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy.maxAttempts"));
  }

  @Test
  public void testWithCustomKeyGenerator() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    StatefulRetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateful()
            .keyGenerator(new MethodArgumentsKeyGenerator() {

              @Override
              public Object getKey(Object[] item) {
                latch.countDown();
                return "foo";
              }
            }).build();

    assertEquals(3, TestUtils.getPropertyValue(interceptor, "retryOperations.retryPolicy.maxAttempts"));
    final AtomicInteger count = new AtomicInteger();
    Foo delegate = createDelegate(interceptor, count);
    Object message = "";
    try {
      delegate.onMessage("", message);
    }
    catch (RuntimeException e) {
      assertEquals("foo", e.getMessage());
    }
    assertEquals(1, count.get());
    assertTrue(latch.await(0, TimeUnit.SECONDS));
  }

  private Foo createDelegate(MethodInterceptor interceptor, final AtomicInteger count) {
    Foo delegate = new Foo() {

      @Override
      public void onMessage(String s, Object message) {
        count.incrementAndGet();
        throw new RuntimeException("foo", new RuntimeException("bar"));
      }

    };
    ProxyFactory factory = new ProxyFactory();
    factory.addAdvisor(new DefaultPointcutAdvisor(Pointcut.TRUE, interceptor));
    factory.setProxyTargetClass(false);
    factory.addInterface(Foo.class);
    factory.setTarget(delegate);
    delegate = (Foo) factory.getProxy();
    return delegate;
  }

  static interface Foo {

    void onMessage(String s, Object message);

  }

}
