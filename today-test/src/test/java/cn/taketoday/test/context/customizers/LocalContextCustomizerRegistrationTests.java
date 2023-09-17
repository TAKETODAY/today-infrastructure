/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.customizers;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig({})
@CustomizeWithFruit
@CustomizeWithFoo
class LocalContextCustomizerRegistrationTests {

  // GlobalFruitContextCustomizerFactory is registered via spring.factories
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
