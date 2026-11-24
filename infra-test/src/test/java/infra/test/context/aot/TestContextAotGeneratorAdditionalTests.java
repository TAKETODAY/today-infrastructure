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

package infra.test.context.aot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.stream.Stream;

import infra.aot.generate.InMemoryGeneratedFiles;
import infra.aot.hint.RuntimeHints;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.ApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextLoadException;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class TestContextAotGeneratorAdditionalTests {

  @Test
  void rejectsMissingLegacyAndNonGenericContextLoaders() throws Exception {
    var generator = generator(true);
    assertThatIllegalArgumentException().isThrownBy(() -> generator.loadContextForAotProcessing(configuration(null)))
            .withMessageContaining("NULL 'contextLoader'");
    assertThatExceptionOfType(TestContextAotException.class)
            .isThrownBy(() -> generator.loadContextForAotProcessing(configuration(mock(ContextLoader.class))))
            .withMessageContaining("must be an AotContextLoader");
    AotContextLoader loader = mock(AotContextLoader.class);
    given(loader.loadContextForAotProcessing(any(), any())).willReturn(mock(ApplicationContext.class));
    assertThatExceptionOfType(TestContextAotException.class)
            .isThrownBy(() -> generator.loadContextForAotProcessing(configuration(loader)))
            .withMessageContaining("GenericApplicationContext");
  }

  @Test
  void loadFailuresPreserveOriginalCause() throws Exception {
    var generator = generator(true);
    AotContextLoader loader = mock(AotContextLoader.class);
    IllegalStateException failure = new IllegalStateException("load failed");
    given(loader.loadContextForAotProcessing(any(), any())).willThrow(failure);
    assertThatExceptionOfType(TestContextAotException.class)
            .isThrownBy(() -> generator.loadContextForAotProcessing(configuration(loader))).withCause(failure);
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      org.mockito.BDDMockito.willThrow(new ContextLoadException(context, failure)).given(loader)
              .loadContextForAotProcessing(any(), any());
      assertThatExceptionOfType(TestContextAotException.class)
              .isThrownBy(() -> generator.loadContextForAotProcessing(configuration(loader))).withCause(failure);
    }
  }

  @Test
  void generationCanContinueAfterFailuresWhenConfigured() {
    var tolerant = generator(false);
    tolerant.processAheadOfTime(Stream.of(Broken.class));
    assertThat(tolerant.getRuntimeHints()).isNotNull();
    assertThatExceptionOfType(TestContextAotException.class)
            .isThrownBy(() -> generator(true).processAheadOfTime(Stream.of(Broken.class)))
            .withMessageContaining("Failed to generate AOT artifacts");
  }

  private TestContextAotGenerator generator(boolean fail) {
    return new TestContextAotGenerator(new InMemoryGeneratedFiles(), new RuntimeHints(), fail);
  }

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "TRACE", "DEBUG", "OFF" })
  void sharedDisabledContextsAndFailuresRespectToleranceAtEveryLogLevel(LogLevel level) {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = TestContextAotGenerator.class.getName();
    var configuration = logging.getLoggerConfiguration(name);
    LogLevel previous = (configuration != null ? configuration.getConfiguredLevel() : null);
    logging.setLogLevel(name, level);
    try {
      generator(false).processAheadOfTime(Stream.of(Enabled.class, Disabled.class));
      generator(false).processAheadOfTime(Stream.of(Disabled.class));
      assertThatExceptionOfType(TestContextAotException.class)
              .isThrownBy(() -> generator(true).processAheadOfTime(Stream.of(Enabled.class, Disabled.class)))
              .withMessageContaining("share an ApplicationContext");
      generator(false).processAheadOfTime(Stream.of(Broken.class));
      TestContextAotGenerator generator = generator(true);
      generator.processAheadOfTime(Stream.of(Enabled.class));
      assertThat(generator.getRuntimeHints().resources().resourcePatternHints()).isNotEmpty();
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  private MergedContextConfiguration configuration(ContextLoader loader) {
    return new MergedContextConfiguration(getClass(), null, null, null, loader);
  }

  @ContextConfiguration(loader = BrokenLoader.class)
  static class Broken { }

  static class BrokenLoader extends AnnotationConfigContextLoader {
    @Override
    protected void customizeContext(GenericApplicationContext context) {
      throw new IllegalStateException("broken context");
    }
  }

  @ContextConfiguration(classes = Config.class)
  @ImportRuntimeHints(Hints.class)
  static class Enabled { }

  @DisabledInAotMode
  @ContextConfiguration(classes = Config.class)
  static class Disabled { }

  @infra.context.annotation.Configuration(proxyBeanMethods = false)
  static class Config { }

  static class Hints implements infra.aot.hint.RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints.resources().registerPattern("diagnostics/*.txt");
    }
  }

}
