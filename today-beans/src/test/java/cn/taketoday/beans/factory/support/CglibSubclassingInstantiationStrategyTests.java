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

package cn.taketoday.beans.factory.support;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/12 17:49
 */
class CglibSubclassingInstantiationStrategyTests {

  private final CglibSubclassingInstantiationStrategy strategy = new CglibSubclassingInstantiationStrategy();

  @Test
  void replaceOverrideMethodInterceptorRejectsNullReturnValueForPrimitives() {
    MyReplacer replacer = new MyReplacer();
    StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of(
            "myBean", new MyBean(),
            "replacer", replacer
    ));

    MethodOverrides methodOverrides = new MethodOverrides();
    Stream.of("getBoolean", "getChar", "getByte", "getShort", "getInt", "getLong", "getFloat", "getDouble")
            .map(methodToOverride -> new ReplaceOverride(methodToOverride, "replacer"))
            .forEach(methodOverrides::addOverride);

    RootBeanDefinition bd = new RootBeanDefinition(MyBean.class);
    bd.setMethodOverrides(methodOverrides);

    MyBean bean = (MyBean) strategy.instantiate(bd, "myBean", beanFactory);

    replacer.reset();
    assertCorrectExceptionThrownBy(bean::getBoolean);
    replacer.returnValue = true;
    assertThat(bean.getBoolean()).isTrue();

    replacer.reset();
    assertCorrectExceptionThrownBy(bean::getChar);
    replacer.returnValue = 'x';
    assertThat(bean.getChar()).isEqualTo('x');

    replacer.reset();
    assertCorrectExceptionThrownBy(bean::getByte);
    replacer.returnValue = 123;
    assertThat(bean.getByte()).isEqualTo((byte) 123);

    replacer.reset();
    assertCorrectExceptionThrownBy(bean::getShort);
    replacer.returnValue = 123;
    assertThat(bean.getShort()).isEqualTo((short) 123);

    replacer.reset();
    assertCorrectExceptionThrownBy(bean::getInt);
    replacer.returnValue = 123;
    assertThat(bean.getInt()).isEqualTo(123);

    replacer.reset();
    assertCorrectExceptionThrownBy(bean::getLong);
    replacer.returnValue = 123;
    assertThat(bean.getLong()).isEqualTo(123L);

    replacer.reset();
    assertCorrectExceptionThrownBy(bean::getFloat);
    replacer.returnValue = 123;
    assertThat(bean.getFloat()).isEqualTo(123f);

    replacer.reset();
    assertCorrectExceptionThrownBy(bean::getDouble);
    replacer.returnValue = 123;
    assertThat(bean.getDouble()).isEqualTo(123d);
  }

  private static void assertCorrectExceptionThrownBy(ThrowableAssert.ThrowingCallable runnable) {
    assertThatIllegalStateException()
            .isThrownBy(runnable)
            .withMessageMatching(
                    "Null return value from MethodReplacer does not match primitive return type for: " +
                            "\\w+ %s\\.\\w+\\(\\)".formatted(Pattern.quote(MyBean.class.getName())));
  }

  static class MyBean {

    boolean getBoolean() {
      return true;
    }

    char getChar() {
      return 'x';
    }

    byte getByte() {
      return 123;
    }

    short getShort() {
      return 123;
    }

    int getInt() {
      return 123;
    }

    long getLong() {
      return 123;
    }

    float getFloat() {
      return 123;
    }

    double getDouble() {
      return 123;
    }
  }

  static class MyReplacer implements MethodReplacer {

    @Nullable
    Object returnValue;

    void reset() {
      this.returnValue = null;
    }

    @Override
    public Object reimplement(Object obj, Method method, Object[] args) {
      return this.returnValue;
    }
  }

}