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

package infra.aop.framework.adapter;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;

import infra.aop.framework.AopConfigException;
import infra.aop.testfixture.advice.MyThrowsHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ThrowsAdviceInterceptorTests {

  @Test
  public void testNoHandlerMethods() {
    // should require one handler method at least
    assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
        new ThrowsAdviceInterceptor(new Object()));
  }

  @Test
  public void testNotInvoked() throws Throwable {
    MyThrowsHandler th = new MyThrowsHandler();
    ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
    Object ret = new Object();
    MethodInvocation mi = mock();
    given(mi.proceed()).willReturn(ret);
    assertThat(ti.invoke(mi)).isEqualTo(ret);
    assertThat(th.getCalls()).isEqualTo(0);
  }

  @Test
  public void testNoHandlerMethodForThrowable() throws Throwable {
    MyThrowsHandler th = new MyThrowsHandler();
    ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
    assertThat(ti.getHandlerMethodCount()).isEqualTo(2);
    Exception ex = new Exception();
    MethodInvocation mi = mock();
    given(mi.proceed()).willThrow(ex);
    assertThatException().isThrownBy(() -> ti.invoke(mi)).isSameAs(ex);
    assertThat(th.getCalls()).isEqualTo(0);
  }

  @Test
  public void testCorrectHandlerUsed() throws Throwable {
    MyThrowsHandler th = new MyThrowsHandler();
    ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
    FileNotFoundException ex = new FileNotFoundException();
    MethodInvocation mi = mock();
    given(mi.getMethod()).willReturn(Object.class.getMethod("hashCode"));
    given(mi.getThis()).willReturn(new Object());
    given(mi.proceed()).willThrow(ex);
    assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> ti.invoke(mi)).isSameAs(ex);
    assertThat(th.getCalls()).isEqualTo(1);
    assertThat(th.getCalls("ioException")).isEqualTo(1);
  }

  @Test
  public void testCorrectHandlerUsedForSubclass() throws Throwable {
    MyThrowsHandler th = new MyThrowsHandler();
    ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
    // Extends RemoteException
    ConnectException ex = new ConnectException("");
    MethodInvocation mi = mock();
    given(mi.proceed()).willThrow(ex);
    assertThatExceptionOfType(ConnectException.class).isThrownBy(() -> ti.invoke(mi)).isSameAs(ex);
    assertThat(th.getCalls()).isEqualTo(1);
    assertThat(th.getCalls("remoteException")).isEqualTo(1);
  }

  @Test
  public void testHandlerMethodThrowsException() throws Throwable {
    final Throwable t = new Throwable();

    @SuppressWarnings("serial")
    MyThrowsHandler th = new MyThrowsHandler() {
      @Override
      public void afterThrowing(RemoteException ex) throws Throwable {
        super.afterThrowing(ex);
        throw t;
      }
    };

    ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
    // Extends RemoteException
    ConnectException ex = new ConnectException("");
    MethodInvocation mi = mock();
    given(mi.proceed()).willThrow(ex);
    assertThatExceptionOfType(Throwable.class).isThrownBy(() -> ti.invoke(mi)).isSameAs(t);
    assertThat(th.getCalls()).isEqualTo(1);
    assertThat(th.getCalls("remoteException")).isEqualTo(1);
  }

}
