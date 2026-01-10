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
@RegisterReflectionForBinding(DeclarativeRuntimeHintsJupiterTests.SampleClassWithGetter.class)
@ImportRuntimeHints(DeclarativeRuntimeHintsJupiterTests.DemoHints.class)
public class DeclarativeRuntimeHintsJupiterTests {

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
