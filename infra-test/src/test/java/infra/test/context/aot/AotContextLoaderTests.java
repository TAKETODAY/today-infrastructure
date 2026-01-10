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
