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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Chris Beams
 */
public class PerformanceMonitorInterceptorTests {

  @Test
  public void testSuffixAndPrefixAssignment() {
    PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor();

    assertThat(interceptor.getPrefix()).isNotNull();
    assertThat(interceptor.getSuffix()).isNotNull();

    interceptor.setPrefix(null);
    interceptor.setSuffix(null);

    assertThat(interceptor.getPrefix()).isNotNull();
    assertThat(interceptor.getSuffix()).isNotNull();
  }

  @Test
  public void testSunnyDayPathLogsPerformanceMetricsCorrectly() throws Throwable {
    MethodInvocation mi = mock(MethodInvocation.class);
    given(mi.getMethod()).willReturn(String.class.getMethod("toString", new Class[0]));

    Logger log = mock(Logger.class);

    PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor(true);
    interceptor.invokeUnderTrace(mi, log);

    verify(log).trace(anyString());
  }

  @Test
  public void testExceptionPathStillLogsPerformanceMetricsCorrectly() throws Throwable {
    MethodInvocation mi = mock(MethodInvocation.class);

    given(mi.getMethod()).willReturn(String.class.getMethod("toString", new Class[0]));
    given(mi.proceed()).willThrow(new IllegalArgumentException());
    Logger log = mock(Logger.class);

    PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor(true);
    assertThatIllegalArgumentException().isThrownBy(() ->
            interceptor.invokeUnderTrace(mi, log));

    verify(log).trace(anyString());
  }

}
