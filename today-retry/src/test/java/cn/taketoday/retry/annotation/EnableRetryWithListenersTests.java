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

import org.junit.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.listener.RetryListenerSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 */
public class EnableRetryWithListenersTests {

  @Test
  public void vanilla() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    Service service = context.getBean(Service.class);
    service.service();
    assertEquals(1, context.getBean(TestConfiguration.class).count);
    context.close();
  }

  @Test
  public void overrideListener() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            TestConfigurationMultipleListeners.class);
    ServiceWithOverriddenListener service = context.getBean(ServiceWithOverriddenListener.class);
    service.service();
    assertEquals(1, context.getBean(TestConfigurationMultipleListeners.class).count1);
    assertEquals(0, context.getBean(TestConfigurationMultipleListeners.class).count2);
    context.close();
  }

  @Configuration
  @EnableRetry(proxyTargetClass = true)
  protected static class TestConfiguration {

    private int count = 0;

    @Bean
    public Service service() {
      return new Service();
    }

    @Bean
    public RetryListener listener() {
      return new RetryListenerSupport() {
        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                Throwable throwable) {
          count++;
        }
      };
    }

  }

  @Configuration
  @EnableRetry(proxyTargetClass = true)
  protected static class TestConfigurationMultipleListeners {

    private int count1 = 0;

    private int count2 = 0;

    @Bean
    public ServiceWithOverriddenListener service() {
      return new ServiceWithOverriddenListener();
    }

    @Bean
    public RetryListener listener1() {
      return new RetryListenerSupport() {
        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                Throwable throwable) {
          count1++;
        }
      };
    }

    @Bean
    public RetryListener listener2() {
      return new RetryListenerSupport() {
        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                Throwable throwable) {
          count2++;
        }
      };
    }

  }

  protected static class Service {

    private int count = 0;

    @Retryable(backoff = @Backoff(delay = 1000))
    public void service() {
      if (count++ < 2) {
        throw new RuntimeException("Planned");
      }
    }

    public int getCount() {
      return count;
    }

  }

  protected static class ServiceWithOverriddenListener {

    private int count = 0;

    @Retryable(backoff = @Backoff(delay = 1000), listeners = "listener1")
    public void service() {
      if (count++ < 2) {
        throw new RuntimeException("Planned");
      }
    }

    public int getCount() {
      return count;
    }

  }

}
