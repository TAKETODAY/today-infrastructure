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

import infra.aot.hint.RuntimeHints;
import infra.context.ApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;

/**
 * Unit tests for {@link AotContextLoader}.
 *
 * @author Sam Brannen
 */
class AotContextLoaderTests {

  /**
   * Verifies that a legacy {@link AotContextLoader} which only overrides
   * is still supported.
   */
  @Test
  void legacyAotContextLoader() throws Exception {
    // Prerequisites
    assertDeclaringClasses(LegacyAotContextLoader.class, LegacyAotContextLoader.class);

    AotContextLoader loader = spy(new LegacyAotContextLoader());
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(), null, null, null, loader);

    RuntimeHints runtimeHints = new RuntimeHints();
    loader.loadContextForAotProcessing(mergedConfig, runtimeHints);

    then(loader).should().loadContextForAotProcessing(mergedConfig, runtimeHints);
  }

  /**
   * Verifies that a modern {@link AotContextLoader} which only overrides
   * {@link AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration, RuntimeHints)
   * is supported.
   */
  @Test
  void runtimeHintsAwareAotContextLoader() throws Exception {
    // Prerequisites
    assertDeclaringClasses(RuntimeHintsAwareAotContextLoader.class, RuntimeHintsAwareAotContextLoader.class);

    AotContextLoader loader = spy(new RuntimeHintsAwareAotContextLoader());
    MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(), null, null, null, loader);

    RuntimeHints runtimeHints = new RuntimeHints();
    loader.loadContextForAotProcessing(mergedConfig, runtimeHints);

    then(loader).should().loadContextForAotProcessing(mergedConfig, runtimeHints);
  }

  private static void assertDeclaringClasses(Class<? extends AotContextLoader> loaderClass,
          Class<?> declaringClassForNewMethod) throws Exception {

    Method newMethod = loaderClass.getMethod("loadContextForAotProcessing", MergedContextConfiguration.class, RuntimeHints.class);

    assertThat(newMethod.getDeclaringClass()).isEqualTo(declaringClassForNewMethod);
  }

  private static class LegacyAotContextLoader extends AbstractAotContextLoader {

    @Override
    public ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig, RuntimeHints runtimeHints) throws Exception {
      return loadContext(mergedConfig);
    }
  }

  private static class RuntimeHintsAwareAotContextLoader extends AbstractAotContextLoader {

    @Override
    public GenericApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig,
            RuntimeHints runtimeHints) {

      return loadContext(mergedConfig);
    }
  }

}
