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

package infra.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import infra.aop.AopInvocationException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 *A null value returned from around advice is very hard to debug if
 * the caller expects a primitive.
 *
 * @author Dave Syer
 */
public class NullPrimitiveTests {

  interface Foo {
    int getValue();
  }

  @Test
  public void testNullPrimitiveWithJdkProxy() {

    class SimpleFoo implements Foo {
      @Override
      public int getValue() {
        return 100;
      }
    }

    SimpleFoo target = new SimpleFoo();
    ProxyFactory factory = new ProxyFactory(target);
    factory.addAdvice((MethodInterceptor) invocation -> null);

    Foo foo = (Foo) factory.getProxy();

    assertThatExceptionOfType(AopInvocationException.class).isThrownBy(() ->
                    foo.getValue())
            .withMessageContaining("Foo.getValue()");
  }

  public static class Bar {
    public int getValue() {
      return 100;
    }
  }

  @Test
  public void testNullPrimitiveWithCglibProxy() {

    Bar target = new Bar();
    ProxyFactory factory = new ProxyFactory(target);
    factory.addAdvice((MethodInterceptor) invocation -> null);

    Bar bar = (Bar) factory.getProxy();

    assertThatExceptionOfType(AopInvocationException.class).isThrownBy(() ->
                    bar.getValue())
            .withMessageContaining("Bar.getValue()");
  }

}
