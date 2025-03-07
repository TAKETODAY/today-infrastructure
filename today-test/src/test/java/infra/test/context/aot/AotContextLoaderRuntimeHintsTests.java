/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.context.aot;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import infra.aot.generate.InMemoryGeneratedFiles;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.RuntimeHints;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.MergedContextConfiguration;

import static infra.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for registering run-time hints within an {@link AotContextLoader}, tested
 * via the {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 */
class AotContextLoaderRuntimeHintsTests {

  @Test
  void aotContextLoaderCanRegisterRuntimeHints() {
    RuntimeHints runtimeHints = new RuntimeHints();
    TestContextAotGenerator generator = new TestContextAotGenerator(new InMemoryGeneratedFiles(), runtimeHints);

    generator.processAheadOfTime(Stream.of(TestCase.class));

    assertThat(reflection().onMethodInvocation(ConfigWithMain.class, "main")).accepts(runtimeHints);
  }

  @ContextConfiguration(classes = ConfigWithMain.class, loader = RuntimeHintsAwareAotContextLoader.class)
  static class TestCase {
  }

  @Configuration(proxyBeanMethods = false)
  static class ConfigWithMain {

    public static void main(String[] args) {
      // Mimics main() method for Spring Boot app
    }
  }

  static class RuntimeHintsAwareAotContextLoader extends AbstractAotContextLoader {

    @Override
    public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
      /* no-op */
    }

    @Override
    public GenericApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig,
            RuntimeHints runtimeHints) throws Exception {

      // Mimics SpringBootContextLoader
      Method mainMethod = mergedConfig.getClasses()[0].getMethod("main", String[].class);
      runtimeHints.reflection().registerMethod(mainMethod, ExecutableMode.INVOKE);

      return loadContext(mergedConfig);
    }
  }

}
