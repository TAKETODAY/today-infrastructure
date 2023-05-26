/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.policy.CircuitBreakerRetryPolicy;
import cn.taketoday.retry.support.RetrySynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerResetTimeoutTests {

  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
          TestConfiguration.class);

  private final TestService serviceInTest = context.getBean(TestService.class);

  @Test
  public void circuitBreakerShouldBeClosedAfterResetTimeout() throws InterruptedException {
    incorrectStep();
    incorrectStep();
    incorrectStep();
    incorrectStep();

    final long timeOfLastFailure = System.currentTimeMillis();
    correctStep(timeOfLastFailure);
    correctStep(timeOfLastFailure);
    correctStep(timeOfLastFailure);
    assertThat((Boolean) serviceInTest.getContext().getAttribute(CircuitBreakerRetryPolicy.CIRCUIT_OPEN)).isFalse();
  }

  private void incorrectStep() {
    doFailedUpload(serviceInTest);
    System.out.println();
  }

  private void correctStep(final long timeOfLastFailure) throws InterruptedException {
    Thread.sleep(6000L);
    printTime(timeOfLastFailure);
    doCorrectUpload(serviceInTest);
    System.out.println();
  }

  private void printTime(final long timeOfLastFailure) {
    System.out.println(String.format("%d ms after last failure", (System.currentTimeMillis() - timeOfLastFailure)));
  }

  private void doFailedUpload(TestService externalService) {
    externalService.service("FAIL");
  }

  private void doCorrectUpload(TestService externalService) {
    externalService.service("");
  }

  @Configuration
  @EnableRetry
  protected static class TestConfiguration {

    @Bean
    public TestService externalService() {
      return new TestService();
    }

  }

  static class TestService {

    private RetryContext context;

    @CircuitBreaker(retryFor = { RuntimeException.class }, openTimeout = 10000, resetTimeout = 15000)
    String service(String payload) {
      this.context = RetrySynchronizationManager.getContext();
      System.out.println("real service called");
      if (payload.contentEquals("FAIL")) {
        throw new RuntimeException("");
      }
      return payload;
    }

    @Recover
    public String recover() {
      System.out.println("recovery action");
      return "";
    }

    public RetryContext getContext() {
      return this.context;
    }

  }

}
