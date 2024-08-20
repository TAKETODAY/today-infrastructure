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

package cn.taketoday.framework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.logging.Logger;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/8/20 15:03
 */
class StartupLoggingTests {

  private final Logger log = mock(Logger.class);

  private MockEnvironment environment;

  @BeforeEach
  void setUp() {
    this.environment = new MockEnvironment();
    this.environment.setProperty("app.version", "1.2.3");
    this.environment.setProperty("app.pid", "42");
  }

  @Test
  void startingFormat() {
    given(this.log.isInfoEnabled()).willReturn(true);
    new StartupLogging(getClass(), this.environment).logStarting(this.log);
    then(this.log).should()
            .info(assertArg(
                    (StringBuilder message) -> assertThat(message.toString()).contains("Starting " + getClass().getSimpleName()
                            + " v1.2.3 using Java " + System.getProperty("java.version") + " with PID 42 (started by "
                            + System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")")));
  }

  @Test
  void startingFormatWhenVersionIsNotAvailable() {
    this.environment.setProperty("app.version", "");
    given(this.log.isInfoEnabled()).willReturn(true);
    new StartupLogging(getClass(), this.environment).logStarting(this.log);
    then(this.log).should()
            .info(assertArg(
                    (StringBuilder message) -> assertThat(message).contains("Starting " + getClass().getSimpleName()
                            + " using Java " + System.getProperty("java.version") + " with PID 42 (started by "
                            + System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")")));
  }

  @Test
  void startingFormatWhenPidIsNotAvailable() {
    this.environment.setProperty("app.pid", "");
    given(this.log.isInfoEnabled()).willReturn(true);
    new StartupLogging(getClass(), this.environment).logStarting(this.log);
    then(this.log).should()
            .info(assertArg((StringBuilder message) ->
                    assertThat(message.toString()).contains("Starting " + getClass().getSimpleName()
                            + " v1.2.3 using Java " + System.getProperty("java.version") + " (started by "
                            + System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")")));
  }

  @Test
  void startingFormatInAotMode() {
    System.setProperty("infra.aot.enabled", "true");
    try {
      given(this.log.isInfoEnabled()).willReturn(true);
      new StartupLogging(getClass(), this.environment).logStarting(this.log);
      then(this.log).should()
              .info(assertArg((StringBuilder message) -> assertThat(message.toString())
                      .contains("Starting AOT-processed " + getClass().getSimpleName() + " v1.2.3 using Java "
                              + System.getProperty("java.version") + " with PID 42 (started by "
                              + System.getProperty("user.name") + " in " + System.getProperty("user.dir") + ")")));

    }
    finally {
      System.clearProperty("infra.aot.enabled");
    }
  }

  @Test
  void startedFormat() {
    given(this.log.isInfoEnabled()).willReturn(true);
    new StartupLogging(getClass(), this.environment).logStarted(this.log, new TestStartup(1345L, "Started"));
    then(this.log).should()
            .info(assertArg((StringBuilder message) -> assertThat(message.toString()).matches("Started " + getClass().getSimpleName()
                    + " in \\d+\\.\\d{1,3} seconds \\(process running for 1.345\\)")));
  }

  @Test
  void startedWithoutUptimeFormat() {
    given(this.log.isInfoEnabled()).willReturn(true);
    new StartupLogging(getClass(), this.environment).logStarted(this.log, new TestStartup(null, "Started"));
    then(this.log).should().info(assertArg((StringBuilder message) -> assertThat(message.toString())
            .matches("Started " + getClass().getSimpleName() + " in \\d+\\.\\d{1,3} seconds")));
  }

  @Test
  void restoredFormat() {
    given(this.log.isInfoEnabled()).willReturn(true);
    new StartupLogging(getClass(), this.environment).logStarted(this.log, new TestStartup(null, "Restored"));
    then(this.log).should().info(
            assertArg((StringBuilder message) -> assertThat(message)
                    .matches("Restored " + getClass().getSimpleName() + " in \\d+\\.\\d{1,3} seconds"))
    );
  }

  static class TestStartup extends Application.Startup {

    private final long startTime = System.currentTimeMillis();

    private final Long uptime;

    private final String action;

    TestStartup(Long uptime, String action) {
      this.uptime = uptime;
      this.action = action;
      started();
    }

    @Override
    protected long startTime() {
      return this.startTime;
    }

    @Override
    protected Long processUptime() {
      return this.uptime;
    }

    @Override
    protected String action() {
      return this.action;
    }

  }

}