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

package infra.test.context.customizers;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.ContextCustomizerFactories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test which verifies support for {@link ContextCustomizerFactory}
 * and {@link ContextCustomizer} when a custom factory is registered declaratively
 * via {@link ContextCustomizerFactories @ContextCustomizerFactories}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig({})
@CustomizeWithFruit
@CustomizeWithFoo
@ContextCustomizerFactories(EnigmaContextCustomizerFactory.class)
@CustomizeWithBar
class ContextCustomizerDeclarativeRegistrationTests {

  // GlobalFruitContextCustomizerFactory is registered via spring.factories
  @Autowired(required = false)
  @Qualifier("global$fruit")
  String fruit;

  @Autowired
  Integer enigma;

  @Autowired(required = false)
  @Qualifier("foo")
  String foo;

  @Autowired(required = false)
  @Qualifier("bar")
  String bar;

  @Test
  void injectedBean() {
    // registered globally via spring.factories
    assertThat(fruit).isEqualTo("apple, banana, cherry");

    // From local @ContextCustomizerFactories
    assertThat(enigma).isEqualTo(42);

    // @ContextCustomizerFactories is not currently supported as a repeatable annotation,
    // and a directly present @ContextCustomizerFactories annotation overrides
    // @ContextCustomizerFactories meta-annotations.
    assertThat(foo).isNull();
    assertThat(bar).isNull();
  }

}
