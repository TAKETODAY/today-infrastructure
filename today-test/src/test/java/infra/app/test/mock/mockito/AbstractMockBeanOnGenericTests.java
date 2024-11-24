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

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.app.test.context.InfraTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockBean} with abstract class and generics.
 *
 * @param <T> type of thing
 * @param <U> type of something
 * @author Madhura Bhave
 */
@InfraTest(classes = AbstractMockBeanOnGenericTests.TestConfiguration.class)
abstract class AbstractMockBeanOnGenericTests<T extends AbstractMockBeanOnGenericTests.Thing<U>, U extends AbstractMockBeanOnGenericTests.Something> {

  @Autowired
  @SuppressWarnings("unused")
  private T thing;

  @MockBean
  private U something;

  @Test
  void mockBeanShouldResolveConcreteType() {
    assertThat(this.something).isInstanceOf(SomethingImpl.class);
  }

  abstract static class Thing<T extends Something> {

    @Autowired
    private T something;

    T getSomething() {
      return this.something;
    }

    void setSomething(T something) {
      this.something = something;
    }

  }

  static class SomethingImpl extends Something {

  }

  static class ThingImpl extends Thing<SomethingImpl> {

  }

  static class Something {

  }

  @Configuration
  static class TestConfiguration {

    @Bean
    ThingImpl thing() {
      return new ThingImpl();
    }

  }

}
