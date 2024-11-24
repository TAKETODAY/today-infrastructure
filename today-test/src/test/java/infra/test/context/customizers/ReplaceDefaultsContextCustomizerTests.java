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
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.ContextCustomizerFactories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig({})
@ContextCustomizerFactories(factories = { FooContextCustomizerFactory.class, BarContextCustomizerFactory.class },
                            mergeMode = ContextCustomizerFactories.MergeMode.REPLACE_DEFAULTS)
class ReplaceDefaultsContextCustomizerTests {

  // GlobalFruitContextCustomizerFactory is registered via spring.factories
  @Autowired(required = false)
  @Qualifier("global$fruit")
  String fruit;

  @Autowired(required = false)
  @Qualifier("foo")
  String foo;

  @Autowired(required = false)
  @Qualifier("bar")
  String bar;

  @Test
  void injectedBean() {
    // MergeMode.REPLACE_DEFAULTS overrides spring.factories lookup
    assertThat(fruit).isNull();

    // From local @ContextCustomizerFactories
    assertThat(foo).isEqualTo("bar");
    assertThat(bar).isEqualTo("baz");
  }

}
