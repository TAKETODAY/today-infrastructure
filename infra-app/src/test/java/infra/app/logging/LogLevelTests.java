/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.logging;

import org.junit.jupiter.api.Test;

import infra.logging.Logger;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LogLevel}.
 *
 * @author Phillip Webb
 */
class LogLevelTests {

  private Logger logger = mock(Logger.class);

  private Exception exception = new Exception();

  @Test
  void logWhenTraceLogsAtTrace() {
    LogLevel.TRACE.log(this.logger, "test");
    LogLevel.TRACE.log(this.logger, "test", this.exception);
    then(this.logger).should().trace((Object) "test", (Throwable) null);
    then(this.logger).should().trace((Object) "test", this.exception);
  }

  @Test
  void logWhenDebugLogsAtDebug() {
    LogLevel.DEBUG.log(this.logger, "test");
    LogLevel.DEBUG.log(this.logger, "test", this.exception);
    then(this.logger).should().debug((Object) "test", (Throwable) null);
    then(this.logger).should().debug((Object) "test", this.exception);
  }

  @Test
  void logWhenInfoLogsAtInfo() {
    LogLevel.INFO.log(this.logger, "test");
    LogLevel.INFO.log(this.logger, "test", this.exception);
    then(this.logger).should().info((Object) "test", (Throwable) null);
    then(this.logger).should().info((Object) "test", this.exception);
  }

  @Test
  void logWhenWarnLogsAtWarn() {
    LogLevel.WARN.log(this.logger, "test");
    LogLevel.WARN.log(this.logger, "test", this.exception);
    then(this.logger).should().warn((Object) "test", (Throwable) null);
    then(this.logger).should().warn((Object) "test", this.exception);
  }

  @Test
  void logWhenErrorLogsAtError() {
    LogLevel.ERROR.log(this.logger, "test");
    LogLevel.ERROR.log(this.logger, "test", this.exception);
    then(this.logger).should().error((Object) "test", (Throwable) null);
    then(this.logger).should().error((Object) "test", this.exception);
  }

  @Test
  void logWhenFatalLogsAtFatal() {
    LogLevel.FATAL.log(this.logger, "test");
    LogLevel.FATAL.log(this.logger, "test", this.exception);
    then(this.logger).should().error((Object) "test", (Throwable) null);
    then(this.logger).should().error((Object) "test", this.exception);
  }

  @Test
  void logWhenOffDoesNotLog() {
    LogLevel.OFF.log(this.logger, "test");
    LogLevel.OFF.log(this.logger, "test", this.exception);
    then(this.logger).shouldHaveNoInteractions();
  }

}
