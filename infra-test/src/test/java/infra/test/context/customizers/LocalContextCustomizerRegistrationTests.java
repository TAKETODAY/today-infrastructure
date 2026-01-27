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

package infra.test.context.customizers;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig({})
@CustomizeWithFruit
@CustomizeWithFoo
class LocalContextCustomizerRegistrationTests {

  // GlobalFruitContextCustomizerFactory is registered via today.strategies
  @Autowired(required = false)
  @Qualifier("global$fruit")
  String fruit;

  @Autowired(required = false)
  @Qualifier("foo")
  String foo;

  @Test
  void injectedBean() {
    assertThat(fruit).isEqualTo("apple, banana, cherry");
    assertThat(foo).isEqualTo("bar");
  }

}
