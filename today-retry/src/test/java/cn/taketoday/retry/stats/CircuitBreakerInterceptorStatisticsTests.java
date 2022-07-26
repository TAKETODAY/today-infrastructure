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

package cn.taketoday.retry.stats;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryStatistics;
import cn.taketoday.retry.annotation.CircuitBreaker;
import cn.taketoday.retry.annotation.EnableRetry;
import cn.taketoday.retry.annotation.Recover;
import cn.taketoday.retry.support.RetrySynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 */
public class CircuitBreakerInterceptorStatisticsTests {

  private static final String RECOVERED = "RECOVERED";

  private static final String RESULT = "RESULT";

  private Service callback;

  private StatisticsRepository repository;

  private AnnotationConfigApplicationContext context;

  @BeforeEach
  public void init() {
    context = new AnnotationConfigApplicationContext(TestConfiguration.class);
    this.callback = context.getBean(Service.class);
    this.repository = context.getBean(StatisticsRepository.class);
    this.callback.setAttemptsBeforeSuccess(1);
  }

  @AfterEach
  public void close() {
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void testCircuitOpenWhenNotRetryable() throws Throwable {
    Object result = callback.service("one");
    RetryStatistics stats = repository.findOne("test");
    // System.err.println(stats);
    assertThat(stats.getStartedCount()).isEqualTo(1);
    assertThat(result).isEqualTo(RECOVERED);
    result = callback.service("two");
    assertThat(result).isEqualTo(RECOVERED);
    assertThat(stats.getRecoveryCount()).describedAs("There should be two recoveries").isEqualTo(2);
    assertThat(stats.getErrorCount()).describedAs("There should only be one error because the circuit is now open")
            .isEqualTo(1);
  }

  @Configuration
  @EnableRetry
  protected static class TestConfiguration {

    @Bean
    public StatisticsRepository repository() {
      return new DefaultStatisticsRepository();
    }

    @Bean
    public StatisticsListener listener(StatisticsRepository repository) {
      return new StatisticsListener(repository);
    }

    @Bean
    public Service service() {
      return new Service();
    }

  }

  protected static class Service {

    private int attemptsBeforeSuccess;

    private Exception exceptionToThrow = new Exception();

    private RetryContext status;

    @CircuitBreaker(label = "test", maxAttempts = 1)
    public Object service(String input) throws Exception {
      this.status = RetrySynchronizationManager.getContext();
      Integer attempts = (Integer) status.getAttribute("attempts");
      if (attempts == null) {
        attempts = 0;
      }
      attempts++;
      this.status.setAttribute("attempts", attempts);
      if (attempts <= this.attemptsBeforeSuccess) {
        throw this.exceptionToThrow;
      }
      return RESULT;
    }

    @Recover
    public Object recover() {
      this.status.setAttribute(RECOVERED, true);
      return RECOVERED;
    }

    public boolean isOpen() {
      return this.status != null && this.status.getAttribute("open") == Boolean.TRUE;
    }

    public void setAttemptsBeforeSuccess(int attemptsBeforeSuccess) {
      this.attemptsBeforeSuccess = attemptsBeforeSuccess;
    }

    public void setExceptionToThrow(Exception exceptionToThrow) {
      this.exceptionToThrow = exceptionToThrow;
    }

  }

}
