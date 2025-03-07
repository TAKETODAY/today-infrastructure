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

package infra.test.context.aot.samples.hints;

import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.annotation.Reflective;
import infra.aot.hint.annotation.RegisterReflectionForBinding;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportRuntimeHints;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Reflective
@RegisterReflectionForBinding(DeclarativeRuntimeHintsSpringJupiterTests.SampleClassWithGetter.class)
@ImportRuntimeHints(DeclarativeRuntimeHintsSpringJupiterTests.DemoHints.class)
public class DeclarativeRuntimeHintsSpringJupiterTests {

  @Test
  void test(@Autowired String foo) {
    assertThat(foo).isEqualTo("bar");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    String foo() {
      return "bar";
    }
  }

  static class DemoHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints.resources().registerPattern("org/example/config/*.txt");
    }

  }

  public static class SampleClassWithGetter {

    public String getName() {
      return null;
    }
  }

}
