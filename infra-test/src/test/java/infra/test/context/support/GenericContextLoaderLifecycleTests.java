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

package infra.test.context.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import infra.aot.hint.RuntimeHints;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.ContextLoadException;
import infra.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GenericContextLoaderLifecycleTests {

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "TRACE", "DEBUG", "OFF" })
  void legacyAndAotContextsHonorTheirLifecycleRegardlessOfLogging(LogLevel level) throws Exception {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = AbstractGenericContextLoader.class.getName();
    LogLevel previous = logging.getLoggerConfiguration(name).getConfiguredLevel();
    logging.setLogLevel(name, level);
    try {
      var loader = new GenericXmlContextLoader();
      try (var context = loader.loadContext(new String[0])) {
        assertThat(context.isActive()).isTrue();
      }
      var merged = new MergedContextConfiguration(getClass(), null, null, null, loader);
      try (var context = loader.loadContextForAotProcessing(merged, new RuntimeHints())) {
        assertThat(context.isActive()).isFalse();
      }
      try (var context = loader.loadContextForAotRuntime(merged,
              applicationContext -> applicationContext.getBeanFactory().registerSingleton("marker", "initialized"))) {
        assertThat(context.isActive()).isTrue();
        assertThat(context.getBean("marker")).isEqualTo("initialized");
      }
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  @Test
  void initializersAreAppliedAndFailuresPreserveTheContext() throws Exception {
    var loader = new GenericXmlContextLoader();
    var raw = configuration(loader, RawInitializer.class);
    try (var context = loader.loadContextForAotRuntime(raw, applicationContext -> { })) {
      assertThat(context.getBean("raw")).isEqualTo("applied");
    }
    var failing = configuration(loader, FailingInitializer.class);
    assertThatExceptionOfType(ContextLoadException.class)
            .isThrownBy(() -> loader.loadContext(failing))
            .withCauseInstanceOf(IllegalStateException.class)
            .satisfies(failure -> ((ConfigurableApplicationContext) failure.getApplicationContext()).close());
  }

  @Test
  void defaultLocationGenerationCanBeDisabled() {
    var loader = new GenericXmlContextLoader() {
      @Override
      protected boolean isGenerateDefaultLocations() {
        return false;
      }
    };
    assertThat(loader.processLocations(getClass())).isEmpty();
  }

  private MergedContextConfiguration configuration(GenericXmlContextLoader loader,
          Class<? extends ApplicationContextInitializer> initializer) {
    return new MergedContextConfiguration(getClass(), null, null, Set.of(initializer), null, loader);
  }

  public static class RawInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(infra.context.ConfigurableApplicationContext applicationContext) {
      applicationContext.getBeanFactory().registerSingleton("raw", "applied");
    }
  }

  public static class FailingInitializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      throw new IllegalStateException("initializer failed");
    }
  }

}
