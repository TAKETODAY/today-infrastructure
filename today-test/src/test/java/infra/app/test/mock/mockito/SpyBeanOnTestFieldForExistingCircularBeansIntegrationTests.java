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
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Import;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.mockito.BDDMockito.then;

/**
 * Test {@link SpyBean @SpyBean} on a test class field can be used to replace existing
 * beans with circular dependencies.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration(classes = SpyBeanOnTestFieldForExistingCircularBeansIntegrationTests.SpyBeanOnTestFieldForExistingCircularBeansConfig.class)
class SpyBeanOnTestFieldForExistingCircularBeansIntegrationTests {

  @SpyBean
  private One one;

  @Autowired
  private Two two;

  @Test
  void beanWithCircularDependenciesCanBeSpied() {
    this.two.callOne();
    then(this.one).should().someMethod();
  }

  @Import({ One.class, Two.class })
  static class SpyBeanOnTestFieldForExistingCircularBeansConfig {

  }

  static class One {

    @Autowired
    @SuppressWarnings("unused")
    private Two two;

    void someMethod() {

    }

  }

  static class Two {

    @Autowired
    private One one;

    void callOne() {
      this.one.someMethod();
    }

  }

}
