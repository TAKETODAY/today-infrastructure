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

package cn.taketoday.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import cn.taketoday.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the {@link DebugInterceptor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class DebugInterceptorTests {

  @Test
  public void testSunnyDayPathLogsCorrectly() throws Throwable {

    MethodInvocation methodInvocation = mock(MethodInvocation.class);

    Logger log = mock(Logger.class);
    given(log.isTraceEnabled()).willReturn(true);

    DebugInterceptor interceptor = new StubDebugInterceptor(log);
    interceptor.invoke(methodInvocation);
    checkCallCountTotal(interceptor);

    verify(log, times(2)).trace(anyString());
  }

  @Test
  public void testExceptionPathStillLogsCorrectly() throws Throwable {

    MethodInvocation methodInvocation = mock(MethodInvocation.class);

    IllegalArgumentException exception = new IllegalArgumentException();
    given(methodInvocation.proceed()).willThrow(exception);

    Logger log = mock(Logger.class);
    given(log.isTraceEnabled()).willReturn(true);

    DebugInterceptor interceptor = new StubDebugInterceptor(log);
    assertThatIllegalArgumentException().isThrownBy(() ->
            interceptor.invoke(methodInvocation));
    checkCallCountTotal(interceptor);

    verify(log).trace(anyString());
    verify(log).trace(anyString(), eq(exception));
  }

  private void checkCallCountTotal(DebugInterceptor interceptor) {
    assertThat(interceptor.getCount()).as("Intercepted call count not being incremented correctly").isEqualTo(1);
  }

  @SuppressWarnings("serial")
  private static final class StubDebugInterceptor extends DebugInterceptor {

    private final Logger log;

    public StubDebugInterceptor(Logger log) {
      super(true);
      this.log = log;
    }

    @Override
    protected Logger getLoggerForInvocation(MethodInvocation invocation) {
      return log;
    }

  }

}
