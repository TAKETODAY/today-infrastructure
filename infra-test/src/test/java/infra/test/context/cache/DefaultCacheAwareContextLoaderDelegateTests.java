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

import java.util.List;

import infra.context.ApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.test.annotation.DirtiesContext.HierarchyMode;
import infra.test.context.ContextLoadException;
import infra.test.context.ApplicationContextFailureProcessor;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.SmartContextLoader;
import infra.test.context.util.TestContextFactoriesUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultCacheAwareContextLoaderDelegateTests {

  private final DefaultContextCache cache = new DefaultContextCache();

  private final DefaultCacheAwareContextLoaderDelegate delegate = new DefaultCacheAwareContextLoaderDelegate(cache);

  @Test
  void cachesLegacyLoaderResultAndDelegatesUsageAndClose() throws Exception {
    ContextLoader loader = mock(ContextLoader.class);
    String[] locations = { "test.xml" };
    MergedContextConfiguration configuration = new MergedContextConfiguration(getClass(), locations, null, null, loader);
    ApplicationContext context = mock(ApplicationContext.class);
    given(loader.loadContext(locations)).willReturn(context);
    assertThat(delegate.getContextCache()).isSameAs(cache);
    assertThat(delegate.isContextLoaded(configuration)).isFalse();
    assertThat(delegate.loadContext(configuration)).isSameAs(context);
    assertThat(delegate.isContextLoaded(configuration)).isTrue();
    assertThat(delegate.loadContext(configuration)).isSameAs(context);
    verify(loader, times(1)).loadContext(locations);
    delegate.registerContextUsage(configuration, getClass());
    assertThat(cache.getContextUsageCount()).isEqualTo(1);
    delegate.unregisterContextUsage(configuration, getClass());
    assertThat(cache.getContextUsageCount()).isZero();
    delegate.closeContext(configuration, HierarchyMode.CURRENT_LEVEL);
    assertThat(delegate.isContextLoaded(configuration)).isFalse();
  }

  @Test
  void smartLoaderUsesMergedConfiguration() throws Exception {
    SmartContextLoader loader = mock(SmartContextLoader.class);
    MergedContextConfiguration configuration = new MergedContextConfiguration(getClass(), null, null, null, loader);
    ApplicationContext context = mock(ApplicationContext.class);
    given(loader.loadContext(configuration)).willReturn(context);
    assertThat(delegate.loadContext(configuration)).isSameAs(context);
    verify(loader).loadContext(configuration);
  }

  @Test
  void failuresAreCountedAndPreventRepeatedLoads() throws Exception {
    SmartContextLoader loader = mock(SmartContextLoader.class);
    MergedContextConfiguration configuration = new MergedContextConfiguration(getClass(), null, null, null, loader);
    IllegalStateException failure = new IllegalStateException("load failed");
    given(loader.loadContext(configuration)).willThrow(failure);
    assertThatIllegalStateException().isThrownBy(() -> delegate.loadContext(configuration))
            .withMessageStartingWith("Failed to load ApplicationContext").withCause(failure);
    assertThat(cache.getFailureCount(configuration)).isEqualTo(1);
    assertThatIllegalStateException().isThrownBy(() -> delegate.loadContext(configuration))
            .withMessageContaining("failure threshold");
    verify(loader, times(1)).loadContext(configuration);
    assertThat(cache.size()).isZero();
  }

  @Test
  void contextLoadExceptionIsUnwrapped() throws Exception {
    SmartContextLoader loader = mock(SmartContextLoader.class);
    MergedContextConfiguration configuration = new MergedContextConfiguration(getClass(), null, null, null, loader);
    IllegalArgumentException failure = new IllegalArgumentException("bean failed");
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      given(loader.loadContext(configuration)).willThrow(new ContextLoadException(context, failure));
      assertThatIllegalStateException().isThrownBy(() -> delegate.loadContext(configuration)).withCause(failure);
    }
  }

  @Test
  void requiresCacheAndConfiguredLoader() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultCacheAwareContextLoaderDelegate(null));
    MergedContextConfiguration configuration = new MergedContextConfiguration(getClass(), null, null, null, null);
    assertThatIllegalStateException().isThrownBy(() -> delegate.loadContext(configuration))
            .withMessageStartingWith("Failed to load ApplicationContext")
            .withCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void failingFailureProcessorsDoNotHideTheOriginalLoadFailure() throws Exception {
    ApplicationContextFailureProcessor broken = mock(ApplicationContextFailureProcessor.class);
    ApplicationContextFailureProcessor successful = mock(ApplicationContextFailureProcessor.class);
    try (GenericApplicationContext context = new GenericApplicationContext();
            var factories = mockStatic(TestContextFactoriesUtils.class)) {
      factories.when(() -> TestContextFactoriesUtils.loadFactoryImplementations(ApplicationContextFailureProcessor.class))
              .thenReturn(List.of(broken, successful));
      var delegate = new DefaultCacheAwareContextLoaderDelegate(new DefaultContextCache());
      SmartContextLoader loader = mock(SmartContextLoader.class);
      var config = new MergedContextConfiguration(getClass(), null, null, null, loader);
      var failure = new IllegalStateException("original");
      given(loader.loadContext(config)).willThrow(new ContextLoadException(context, failure));
      doThrow(new IllegalArgumentException("processor")).when(broken).processLoadFailure(context, failure);
      assertThatIllegalStateException().isThrownBy(() -> delegate.loadContext(config)).withCause(failure);
      verify(successful).processLoadFailure(context, failure);
    }
  }

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "TRACE", "DEBUG", "OFF" })
  void cacheHitsAndLoadFailuresAreIndependentOfLogging(LogLevel level) throws Exception {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = DefaultCacheAwareContextLoaderDelegate.class.getName();
    LogLevel previous = logging.getLoggerConfiguration(name).getConfiguredLevel();
    logging.setLogLevel(name, level);
    try {
      cachesLegacyLoaderResultAndDelegatesUsageAndClose();
      contextLoadExceptionIsUnwrapped();
    }
    finally {
      cache.clear();
      logging.setLogLevel(name, previous);
    }
  }

}
