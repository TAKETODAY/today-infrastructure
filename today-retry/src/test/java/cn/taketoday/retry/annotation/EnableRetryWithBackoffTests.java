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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableAspectJAutoProxy;
import cn.taketoday.retry.backoff.Sleeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Dave Syer
 */
public class EnableRetryWithBackoffTests {

  @Test
  public void vanilla() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    Service service = context.getBean(Service.class);
    service.service();
    assertEquals("[1000, 1000]", context.getBean(PeriodSleeper.class).getPeriods().toString());
    assertEquals(3, service.getCount());
    context.close();
  }

  @Test
  public void type() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    RandomService service = context.getBean(RandomService.class);
    service.service();
    List<Long> periods = context.getBean(PeriodSleeper.class).getPeriods();
    assertTrue("Wrong periods: " + periods, periods.get(0) > 1000);
    assertEquals(3, service.getCount());
    context.close();
  }

  @Test
  public void exponential() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    ExponentialService service = context.getBean(ExponentialService.class);
    service.service();
    assertEquals(3, service.getCount());
    assertEquals("[1000, 1100]", context.getBean(PeriodSleeper.class).getPeriods().toString());
    context.close();
  }

  @Test
  public void randomExponential() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    ExponentialRandomService service = context.getBean(ExponentialRandomService.class);
    service.service(1);
    assertEquals(3, service.getCount());
    List<Long> periods = context.getBean(PeriodSleeper.class).getPeriods();
    assertNotEquals("[1000, 1100]", context.getBean(PeriodSleeper.class).getPeriods().toString());
    assertTrue("Wrong periods: " + periods, periods.get(0) > 1000);
    assertTrue("Wrong periods: " + periods, periods.get(1) > 1100 && periods.get(1) < 1210);
    context.close();
  }

  @Test
  public void randomExponentialExpression() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    ExponentialRandomExpressionService service = context.getBean(ExponentialRandomExpressionService.class);
    service.service(1);
    assertEquals(3, service.getCount());
    List<Long> periods = context.getBean(PeriodSleeper.class).getPeriods();
    assertNotEquals("[1000, 1100]", context.getBean(PeriodSleeper.class).getPeriods().toString());
    assertTrue("Wrong periods: " + periods, periods.get(0) > 1000);
    assertTrue("Wrong periods: " + periods, periods.get(1) > 1100 && periods.get(1) < 1210);
    context.close();
  }

  @Configuration
  @EnableRetry
  @EnableAspectJAutoProxy(proxyTargetClass = true)
  protected static class TestConfiguration {

    @Bean
    public PeriodSleeper sleper() {
      return new PeriodSleeper();
    }

    @Bean
    public Service service() {
      return new Service();
    }

    @Bean
    public RandomService retryable() {
      return new RandomService();
    }

    @Bean
    public ExponentialRandomService stateful() {
      return new ExponentialRandomService();
    }

    @Bean
    public ExponentialService excludes() {
      return new ExponentialService();
    }

    @Bean
    public ExponentialRandomExpressionService statefulExpression() {
      return new ExponentialRandomExpressionService();
    }

  }

  @SuppressWarnings("serial")
  protected static class PeriodSleeper implements Sleeper {

    private List<Long> periods = new ArrayList<Long>();

    @Override
    public void sleep(long period) throws InterruptedException {
      periods.add(period);
    }

    private List<Long> getPeriods() {
      return periods;
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

  @Retryable(backoff = @Backoff(delay = 1000, maxDelay = 2000))
  protected static class RandomService {

    private int count = 0;

    public void service() {
      if (count++ < 2) {
        throw new RuntimeException("Planned");
      }
    }

    public int getCount() {
      return count;
    }

  }

  protected static class ExponentialService {

    private int count = 0;

    @Retryable(backoff = @Backoff(delay = 1000, maxDelay = 2000, multiplier = 1.1))
    public void service() {
      if (count++ < 2) {
        throw new IllegalStateException("Planned");
      }
    }

    public int getCount() {
      return count;
    }

  }

  protected static class ExponentialRandomService {

    private int count = 0;

    @Retryable(backoff = @Backoff(delay = 1000, maxDelay = 2000, multiplier = 1.1, random = true))
    public void service(int value) {
      if (count++ < 2) {
        throw new RuntimeException("Planned");
      }
    }

    public int getCount() {
      return count;
    }

  }

  protected static class ExponentialRandomExpressionService {

    private int count = 0;

    @Retryable(backoff = @Backoff(delay = 1000, maxDelay = 2000, multiplier = 1.1, randomExpression = "#{true}"))
    public void service(int value) {
      if (count++ < 2) {
        throw new RuntimeException("Planned");
      }
    }

    public int getCount() {
      return count;
    }

  }

}
