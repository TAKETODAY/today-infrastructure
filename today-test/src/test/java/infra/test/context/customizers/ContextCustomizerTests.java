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

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.test.context.BootstrapWith;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.AbstractTestContextBootstrapper;
import infra.test.context.support.ContextCustomizerFactories;
import infra.test.context.support.DefaultTestContextBootstrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test which verifies support for {@link ContextCustomizerFactory}
 * and {@link ContextCustomizer} when a custom factory is registered by overriding
 * {@link AbstractTestContextBootstrapper#getContextCustomizerFactories} and
 * additional factories are registered declaratively via
 * {@link ContextCustomizerFactories @ContextCustomizerFactories}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 */
@JUnitConfig({})
@CustomizeWithFoo
@BootstrapWith(ContextCustomizerTests.EnigmaTestContextBootstrapper.class)
@CustomizeWithBar
class ContextCustomizerTests {

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
    // Local Bootstrapper overrides spring.factories lookup
    assertThat(fruit).isNull();

    // From local Bootstrapper
    assertThat(enigma).isEqualTo(42);

    // From local @ContextCustomizerFactories
    assertThat(foo).isEqualTo("bar");

    // @ContextCustomizerFactories is not currently supported as a repeatable annotation.
    assertThat(bar).isNull();
  }

  static class EnigmaTestContextBootstrapper extends DefaultTestContextBootstrapper {

    @Override
    protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
      return List.of(new EnigmaContextCustomizerFactory());
    }
  }

}
