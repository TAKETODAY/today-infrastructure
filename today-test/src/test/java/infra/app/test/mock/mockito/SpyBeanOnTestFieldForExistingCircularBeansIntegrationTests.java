/*
 * Copyright 2017 - 2026 the TODAY authors.
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
