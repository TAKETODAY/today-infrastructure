/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.context.InfraTest;

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
