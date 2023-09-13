/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableAspectJAutoProxy;
import cn.taketoday.retry.backoff.Sleeper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
public class EnableRetryWithBackoffTests {

  @Test
  public void vanilla() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    Service service = context.getBean(Service.class);
    service.service();
    assertThat(context.getBean(PeriodSleeper.class).getPeriods().toString()).isEqualTo("[1000, 1000]");
    assertThat(service.getCount()).isEqualTo(3);
    context.close();
  }

  @Test
  public void type() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    RandomService service = context.getBean(RandomService.class);
    service.service();
    List<Long> periods = context.getBean(PeriodSleeper.class).getPeriods();
    assertThat(periods.get(0) > 1000).describedAs("Wrong periods: %s" + periods).isTrue();
    assertThat(service.getCount()).isEqualTo(3);
    context.close();
  }

  @Test
  public void exponential() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    ExponentialService service = context.getBean(ExponentialService.class);
    service.service();
    assertThat(service.getCount()).isEqualTo(3);
    assertThat(context.getBean(PeriodSleeper.class).getPeriods().toString()).isEqualTo("[1000, 1100]");
    context.close();
  }

  @Test
  public void randomExponential() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    ExponentialRandomService service = context.getBean(ExponentialRandomService.class);
    service.service(1);
    assertThat(service.getCount()).isEqualTo(3);
    List<Long> periods = context.getBean(PeriodSleeper.class).getPeriods();
    assertThat(context.getBean(PeriodSleeper.class).getPeriods().toString()).isNotEqualTo("[1000, 1100]");
    assertThat(periods.get(0) > 1000).describedAs("Wrong periods: %s", periods).isTrue();
    assertThat(periods.get(1) > 1100 && periods.get(1) < 1210).describedAs("Wrong periods: %s", periods)
            .isTrue();
    context.close();
  }

  @Test
  public void randomExponentialExpression() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    ExponentialRandomExpressionService service = context.getBean(ExponentialRandomExpressionService.class);
    service.service(1);
    assertThat(service.getCount()).isEqualTo(3);
    List<Long> periods = context.getBean(PeriodSleeper.class).getPeriods();
    assertThat(context.getBean(PeriodSleeper.class).getPeriods().toString()).isNotEqualTo("[1000, 1100]");
    assertThat(periods.get(0) > 1000).describedAs("Wrong periods: %s", periods).isTrue();
    assertThat(periods.get(1) > 1100 && periods.get(1) < 1210).describedAs("Wrong periods: %s", periods)
            .isTrue();
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

    private final List<Long> periods = new ArrayList<>();

    @Override
    public void sleep(long period) {
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
