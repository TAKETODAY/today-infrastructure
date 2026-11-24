/*
 * Copyright 2002-present the original author or authors.
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

package infra.aop.framework.adapter;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.stream.Stream;

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

  @ParameterizedTest
  @MethodSource("invalidHandlers")
  void rejectsInvalidHandlerSignatures(Object handler, String message) {
    assertThatExceptionOfType(AopConfigException.class)
            .isThrownBy(() -> new ThrowsAdviceInterceptor(handler))
            .withMessageContaining(message);
  }

  static Stream<Arguments> invalidHandlers() {
    return Stream.of(
            Arguments.of(new Object() {
              public void afterThrowing(String ex) { }
            }, "single argument must be a Throwable subclass"),
            Arguments.of(new Object() {
              public void afterThrowing(String method, Object[] args, Object target, Exception ex) { }
            }, "four arguments must be Method, Object[], target, throwable"),
            Arguments.of(new Object() {
              public void afterThrowing() { }
            }, "Unsupported afterThrowing signature"),
            Arguments.of(new Object() {
              public void afterThrowing(Exception ex) { }

              public void afterThrowing(Method method, Object[] args, Object target, Exception ex) { }
            }, "Only one afterThrowing method per specific Throwable subclass"));
  }

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
