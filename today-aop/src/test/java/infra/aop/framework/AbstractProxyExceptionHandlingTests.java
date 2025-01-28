/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Objects;

import infra.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Mikaël Francoeur
 * @author Sam Brannen
 * @see JdkProxyExceptionHandlingTests
 * @see CglibProxyExceptionHandlingTests
 */
abstract class AbstractProxyExceptionHandlingTests {

  private static final RuntimeException uncheckedException = new RuntimeException();

  private static final DeclaredCheckedException declaredCheckedException = new DeclaredCheckedException();

  private static final UndeclaredCheckedException undeclaredCheckedException = new UndeclaredCheckedException();

  protected final MyClass target = mock();

  protected final ProxyFactory proxyFactory = new ProxyFactory(target);

  protected MyInterface proxy;

  private Throwable throwableSeenByCaller;

  @BeforeEach
  void clear() {
    Mockito.clearInvocations(target);
  }

  protected abstract void assertProxyType(Object proxy);

  private void invokeProxy() {
    throwableSeenByCaller = catchThrowable(() -> Objects.requireNonNull(proxy).doSomething());
  }

  @SuppressWarnings("SameParameterValue")
  private static Answer<?> sneakyThrow(Throwable throwable) {
    return invocation -> {
      throw throwable;
    };
  }

  @Nested
  class WhenThereIsOneInterceptorTests {

    @Nullable
    private Throwable throwableSeenByInterceptor;

    @BeforeEach
    void beforeEach() {
      proxyFactory.addAdvice(captureThrowable());
      proxy = (MyInterface) proxyFactory.getProxy(getClass().getClassLoader());
      assertProxyType(proxy);
    }

    @Test
    void targetThrowsUndeclaredCheckedException() throws DeclaredCheckedException {
      willAnswer(sneakyThrow(undeclaredCheckedException)).given(target).doSomething();
      invokeProxy();
      assertThat(throwableSeenByInterceptor).isSameAs(undeclaredCheckedException);
      assertThat(throwableSeenByCaller)
              .isInstanceOf(UndeclaredThrowableException.class)
              .cause().isSameAs(undeclaredCheckedException);
    }

    @Test
    void targetThrowsDeclaredCheckedException() throws DeclaredCheckedException {
      willThrow(declaredCheckedException).given(target).doSomething();
      invokeProxy();
      assertThat(throwableSeenByInterceptor).isSameAs(declaredCheckedException);
      assertThat(throwableSeenByCaller).isSameAs(declaredCheckedException);
    }

    @Test
    void targetThrowsUncheckedException() throws DeclaredCheckedException {
      willThrow(uncheckedException).given(target).doSomething();
      invokeProxy();
      assertThat(throwableSeenByInterceptor).isSameAs(uncheckedException);
      assertThat(throwableSeenByCaller).isSameAs(uncheckedException);
    }

    private MethodInterceptor captureThrowable() {
      return invocation -> {
        try {
          return invocation.proceed();
        }
        catch (Exception ex) {
          throwableSeenByInterceptor = ex;
          throw ex;
        }
      };
    }
  }

  @Nested
  class WhenThereAreNoInterceptorsTests {

    @BeforeEach
    void beforeEach() {
      proxy = (MyInterface) proxyFactory.getProxy(getClass().getClassLoader());
      assertProxyType(proxy);
    }

    @Test
    void targetThrowsUndeclaredCheckedException() throws DeclaredCheckedException {
      willAnswer(sneakyThrow(undeclaredCheckedException)).given(target).doSomething();
      invokeProxy();
      assertThat(throwableSeenByCaller)
              .isInstanceOf(UndeclaredThrowableException.class)
              .cause().isSameAs(undeclaredCheckedException);
    }

    @Test
    void targetThrowsDeclaredCheckedException() throws DeclaredCheckedException {
      willThrow(declaredCheckedException).given(target).doSomething();
      invokeProxy();
      assertThat(throwableSeenByCaller).isSameAs(declaredCheckedException);
    }

    @Test
    void targetThrowsUncheckedException() throws DeclaredCheckedException {
      willThrow(uncheckedException).given(target).doSomething();
      invokeProxy();
      assertThat(throwableSeenByCaller).isSameAs(uncheckedException);
    }
  }

  interface MyInterface {

    void doSomething() throws DeclaredCheckedException;
  }

  static class MyClass implements MyInterface {

    @Override
    public void doSomething() throws DeclaredCheckedException {
      throw declaredCheckedException;
    }
  }

  @SuppressWarnings("serial")
  private static class UndeclaredCheckedException extends Exception {
  }

  @SuppressWarnings("serial")
  private static class DeclaredCheckedException extends Exception {
  }

}
