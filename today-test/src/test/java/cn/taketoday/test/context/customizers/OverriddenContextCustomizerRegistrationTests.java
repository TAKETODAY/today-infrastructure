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
import cn.taketoday.test.context.support.ContextCustomizerFactories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ContextCustomizerFactories(factories = { BarContextCustomizerFactory.class, EnigmaContextCustomizerFactory.class },
                            inheritFactories = false)
class OverriddenContextCustomizerRegistrationTests extends LocalContextCustomizerRegistrationTests {

  @Autowired
  @Qualifier("bar")
  String bar;

  @Autowired
  Integer enigma;

  @Override
  @Test
  void injectedBean() {
    // globally registered via spring.factories
    assertThat(fruit).isEqualTo("apple, banana, cherry");

    // Overridden by this subclass (inheritFactories = false)
    assertThat(foo).isNull();

    // Local to this subclass
    assertThat(bar).isEqualTo("baz");
    assertThat(enigma).isEqualTo(42);
  }

}
