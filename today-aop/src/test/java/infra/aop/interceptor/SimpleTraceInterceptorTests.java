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

package infra.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import infra.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the {@link SimpleTraceInterceptor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class SimpleTraceInterceptorTests {

  @Test
  public void testSunnyDayPathLogsCorrectly() throws Throwable {
    MethodInvocation mi = mock(MethodInvocation.class);
    given(mi.getMethod()).willReturn(String.class.getMethod("toString"));
    given(mi.getThis()).willReturn(this);

    Logger log = mock(Logger.class);

    SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);
    interceptor.invokeUnderTrace(mi, log);

    verify(log, times(2)).trace(anyString());
  }

  @Test
  public void testExceptionPathStillLogsCorrectly() throws Throwable {
    MethodInvocation mi = mock(MethodInvocation.class);
    given(mi.getMethod()).willReturn(String.class.getMethod("toString"));
    given(mi.getThis()).willReturn(this);
    IllegalArgumentException exception = new IllegalArgumentException();
    given(mi.proceed()).willThrow(exception);

    Logger log = mock(Logger.class);

    final SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);
    assertThatIllegalArgumentException().isThrownBy(() ->
            interceptor.invokeUnderTrace(mi, log));

    verify(log).trace(anyString());
    verify(log).trace(anyString(), eq(exception));
  }

}
