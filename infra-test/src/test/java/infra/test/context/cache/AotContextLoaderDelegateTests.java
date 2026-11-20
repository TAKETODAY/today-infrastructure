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

package infra.test.context.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import infra.aot.AotDetector;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.SmartContextLoader;
import infra.test.context.aot.AotContextLoader;
import infra.test.context.aot.AotTestContextInitializers;
import infra.test.context.aot.TestContextAotException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class AotContextLoaderDelegateTests {

  @Test
  void generatedConfigurationIsCachedAndUsageIsTracked() throws Exception {
    try (var initializers = mockConstruction(AotTestContextInitializers.class, (mock, context) -> {
      given(mock.getContextInitializer(getClass())).willReturn(new Initializer());
      given(mock.getContextInitializerClass(getClass())).willReturn((Class) Initializer.class);
    }); var aot = mockStatic(AotDetector.class)) {
      aot.when(AotDetector::useGeneratedArtifacts).thenReturn(true);
      DefaultContextCache cache = new DefaultContextCache();
      var delegate = new DefaultCacheAwareContextLoaderDelegate(cache);
      AotContextLoader loader = mock(AotContextLoader.class);
      MergedContextConfiguration original = new MergedContextConfiguration(getClass(), null, null, null, loader);
      try (GenericApplicationContext application = new GenericApplicationContext()) {
        application.refresh();
        given(loader.loadContextForAotRuntime(org.mockito.ArgumentMatchers.eq(original), org.mockito.ArgumentMatchers.any()))
                .willReturn(application);
        assertThat(delegate.loadContext(original)).isSameAs(application);
        assertThat(delegate.loadContext(original)).isSameAs(application);
        assertThat(delegate.isContextLoaded(original)).isTrue();
        delegate.registerContextUsage(original, getClass());
        assertThat(cache.getContextUsageCount()).isEqualTo(1);
        delegate.unregisterContextUsage(original, getClass());
        delegate.closeContext(original, null);
        assertThat(delegate.isContextLoaded(original)).isFalse();
      }
      assertThat(initializers.constructed()).hasSize(1);
    }
  }

  @Test
  void requiresGeneratedInitializerInAotMode() {
    try (var initializers = mockConstruction(AotTestContextInitializers.class); var aot = mockStatic(AotDetector.class)) {
      aot.when(AotDetector::useGeneratedArtifacts).thenReturn(true);
      var delegate = new DefaultCacheAwareContextLoaderDelegate(new DefaultContextCache());
      var original = new MergedContextConfiguration(getClass(), null, null, null, null);
      assertThatIllegalStateException().isThrownBy(() -> delegate.loadContext(original))
              .withMessageContaining("Failed to load AOT ApplicationContextInitializer class");
      assertThat(initializers.constructed()).hasSize(1);
    }
  }

  @Test
  void aotLoaderRequiresInitializerAndGenericContext() throws Exception {
    try (var initializers = mockConstruction(AotTestContextInitializers.class)) {
      var delegate = new DefaultCacheAwareContextLoaderDelegate(new DefaultContextCache());
      SmartContextLoader legacy = mock(SmartContextLoader.class);
      var original = new MergedContextConfiguration(getClass(), null, null, null, legacy);
      var configuration = new AotMergedContextConfiguration(getClass(), Initializer.class, original, delegate);
      assertThatIllegalStateException().isThrownBy(() -> delegate.loadContextInAotMode(configuration))
              .withMessageContaining("Failed to load AOT ApplicationContextInitializer");
      AotTestContextInitializers mappings = initializers.constructed().get(0);
      Initializer initializer = new Initializer();
      given(mappings.getContextInitializer(getClass())).willReturn(initializer);
      assertThatExceptionOfType(TestContextAotException.class)
              .isThrownBy(() -> delegate.loadContextInAotMode(configuration)).withMessageContaining("must be an AotContextLoader");
      AotContextLoader loader = mock(AotContextLoader.class);
      var supported = new MergedContextConfiguration(getClass(), null, null, null, loader);
      var aotConfiguration = new AotMergedContextConfiguration(getClass(), Initializer.class, supported, delegate);
      given(loader.loadContextForAotRuntime(supported, initializer)).willReturn(mock(ApplicationContext.class));
      assertThatExceptionOfType(TestContextAotException.class)
              .isThrownBy(() -> delegate.loadContextInAotMode(aotConfiguration)).withMessageContaining("GenericApplicationContext");
      verify(loader).loadContextForAotRuntime(supported, initializer);
      assertThat(configuration.getOriginal()).isSameAs(original);
      assertThat(configuration).isEqualTo(configuration).isEqualTo(aotConfiguration).isNotEqualTo(null).isNotEqualTo(original);
      assertThat(configuration.hashCode()).isEqualTo(aotConfiguration.hashCode());
      assertThat(configuration.toString()).contains("original", "contextInitializerClass");
    }
  }

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "TRACE", "DEBUG", "OFF" })
  void generatedContextLoadingDoesNotDependOnLogging(LogLevel level) throws Exception {
    var logging = LoggingSystem.get(getClass().getClassLoader());
    String name = DefaultCacheAwareContextLoaderDelegate.class.getName();
    var configuration = logging.getLoggerConfiguration(name);
    var previous = configuration != null ? configuration.getConfiguredLevel() : null;
    logging.setLogLevel(name, level);
    try {
      generatedConfigurationIsCachedAndUsageIsTracked();
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  static class Initializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) { }
  }

}
