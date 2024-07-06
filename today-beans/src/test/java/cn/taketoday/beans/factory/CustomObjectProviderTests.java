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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 5.0
 */
public class CustomObjectProviderTests {

  @Test
  void getObject() {
    TestBean tb1 = new TestBean("tb1");

    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public TestBean get() throws BeansException {
        return tb1;
      }
    };

    assertThat(provider.get()).isSameAs(tb1);
    assertThat(provider.getIfAvailable()).isSameAs(tb1);
    assertThat(provider.getIfUnique()).isSameAs(tb1);
  }

  @Test
  void noObject() {
    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public TestBean get() throws BeansException {
        throw new NoSuchBeanDefinitionException(Object.class);
      }
    };

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(provider::get);
    assertThat(provider.getIfAvailable()).isNull();
    assertThat(provider.getIfUnique()).isNull();
  }

  @Test
  void noUniqueObject() {
    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public TestBean get() throws BeansException {
        throw new NoUniqueBeanDefinitionException(Object.class);
      }
    };

    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::get);
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getIfAvailable);
    assertThat(provider.getIfUnique()).isNull();
  }

  @Test
  void emptyStream() {
    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public Stream<TestBean> stream() {
        return Stream.empty();
      }
    };

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(provider::get);
    assertThat(provider.getIfAvailable()).isNull();
    assertThat(provider.getIfUnique()).isNull();
  }

  @Test
  void streamWithOneObject() {
    TestBean tb1 = new TestBean("tb1");

    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public Stream<TestBean> stream() {
        return Stream.of(tb1);
      }
    };

    assertThat(provider.get()).isSameAs(tb1);
    assertThat(provider.getIfAvailable()).isSameAs(tb1);
    assertThat(provider.getIfUnique()).isSameAs(tb1);
  }

  @Test
  void streamWithTwoObjects() {
    TestBean tb1 = new TestBean("tb1");
    TestBean tb2 = new TestBean("tb2");

    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public Stream<TestBean> stream() {
        return Stream.of(tb1, tb2);
      }
    };

    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::get);
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getIfAvailable);
    assertThat(provider.getIfUnique()).isNull();
  }

}
