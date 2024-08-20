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

package cn.taketoday.framework.logging;

import org.junit.jupiter.api.Test;

import cn.taketoday.logging.Logger;

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
