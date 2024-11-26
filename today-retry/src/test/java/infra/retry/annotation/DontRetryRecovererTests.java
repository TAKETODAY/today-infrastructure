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

package infra.retry.annotation;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gary Russell
 * @since 1.3.4
 */
@JUnitConfig
public class DontRetryRecovererTests {

  @Test
  void dontRetry(@Autowired Service service) {
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> service.foo("x")).withMessage("test");
    assertThat(service.getCallCount()).isEqualTo(3);
    assertThat(service.getRecoverCount()).isEqualTo(1);
  }

  @Configuration
  @EnableRetry
  public static class Config {

    @Bean
    Service service() {
      return new Service();
    }

  }

  @Retryable
  public static class Service {

    int callCount;

    int recoverCount;

    public void foo(String in) {
      callCount++;
      throw new RuntimeException();
    }

    @Recover
    public void recover(Exception ex, String in) {
      this.recoverCount++;
      throw new RuntimeException("test");
    }

    public int getCallCount() {
      return callCount;
    }

    public int getRecoverCount() {
      return recoverCount;
    }

  }

}
