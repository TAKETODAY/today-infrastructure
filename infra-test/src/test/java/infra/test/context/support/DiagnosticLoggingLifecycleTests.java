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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import infra.aot.hint.RuntimeHints;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.context.ConfigurableApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.support.GenericApplicationContext;
import infra.test.annotation.DirtiesContext;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.ActiveProfiles;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextCustomizerFactories;
import infra.test.context.TestContext;
import infra.test.context.web.AnnotationConfigWebContextLoader;
import infra.test.context.web.WebMergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DiagnosticLoggingLifecycleTests {

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "TRACE", "DEBUG", "OFF" })
  void contextLoadingAndDirtyLifecycleRemainConsistentAtEachLogLevel(LogLevel level) throws Exception {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = "infra.test.context";
    LogLevel previous = logging.getLoggerConfiguration(name).getConfiguredLevel();
    logging.setLogLevel(name, level);
    try {
      var configuration = new MergedContextConfiguration(Dirty.class, null, null, null, null);
      AnnotationConfigContextLoader loader = new AnnotationConfigContextLoader();
      try (GenericApplicationContext context = loader.loadContextForAotProcessing(configuration, new RuntimeHints())) {
        assertThat(context.isActive()).isFalse();
      }
      try (GenericApplicationContext context = loader.loadContextForAotRuntime(configuration, application -> { })) {
        assertThat(context.isActive()).isTrue();
      }
      AnnotationConfigWebContextLoader webLoader = new AnnotationConfigWebContextLoader();
      var webConfiguration = new WebMergedContextConfiguration(configuration);
      try (ConfigurableApplicationContext context = (ConfigurableApplicationContext)
              webLoader.loadContextForAotProcessing(webConfiguration, new RuntimeHints())) {
        assertThat(context.isActive()).isFalse();
      }
      try (GenericApplicationContext context = webLoader.loadContextForAotRuntime(webConfiguration, application -> { })) {
        assertThat(context.isActive()).isTrue();
      }
      TestContext context = mock(TestContext.class);
      org.mockito.BDDMockito.<Class<?>>given(context.getTestClass()).willReturn(Dirty.class);
      given(context.getTestMethod()).willReturn(Dirty.class.getDeclaredMethod("test"));
      var before = new DirtiesContextBeforeModesTestExecutionListener();
      var after = new DirtiesContextTestExecutionListener();
      before.beforeTestClass(context);
      before.beforeTestMethod(context);
      after.afterTestMethod(context);
      after.afterTestClass(context);
      verify(context, times(2)).markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE);
      verify(context, times(2)).setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE, true);
      DefaultActiveProfilesResolver profiles = new DefaultActiveProfilesResolver();
      assertThat(profiles.resolve(Dirty.class)).isEmpty();
      assertThat(profiles.resolve(Configured.class)).containsExactly("diagnostics");
      for (Class<?> type : new Class<?>[] { Configured.class, Dirty.class,
              TestContextBootstrapperAdditionalTests.MissingListeners.class,
              TestContextBootstrapperAdditionalTests.MissingFactories.class,
              TestContextBootstrapperAdditionalTests.DuplicateFactories.class }) {
        DefaultTestContextBootstrapper bootstrapper = new DefaultTestContextBootstrapper();
        bootstrapper.setBootstrapContext(new DefaultBootstrapContext(type, mock(CacheAwareContextLoaderDelegate.class)));
        bootstrapper.getTestExecutionListeners();
        bootstrapper.buildMergedContextConfiguration();
      }
      DelegatingSmartContextLoader delegating = new DelegatingSmartContextLoader();
      var defaults = new infra.test.context.ContextConfigurationAttributes(getClass());
      delegating.processContextConfiguration(defaults);
      assertThat(defaults.getClasses()).containsExactly(Configuration.class);
      var initialized = new MergedContextConfiguration(Configured.class, null, null,
              Set.of(Initializer.class), null, delegating);
      try (ConfigurableApplicationContext loaded = (ConfigurableApplicationContext) delegating.loadContext(initialized)) {
        assertThat(loaded.isActive()).isTrue();
      }
      try (ConfigurableApplicationContext loaded = (ConfigurableApplicationContext)
              delegating.loadContextForAotProcessing(initialized, new RuntimeHints())) {
        assertThat(loaded.isActive()).isFalse();
      }
      try (ConfigurableApplicationContext loaded = (ConfigurableApplicationContext)
              delegating.loadContextForAotRuntime(initialized, application -> { })) {
        assertThat(loaded.isActive()).isTrue();
      }
      var properties = TestPropertySourceUtils.buildMergedTestPropertySources(DuplicateProperties.class);
      assertThat(properties.getProperties()).containsExactly("diagnostics=true");
      var environment = new infra.mock.env.MockEnvironment();
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, "first=1");
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, "second=2");
      assertThat(environment.getProperty("first")).isEqualTo("1");
      assertThat(environment.getProperty("second")).isEqualTo("2");
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
  static class Dirty {
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void test() { }
  }

  @ActiveProfiles("diagnostics")
  @ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = Configuration.class)
  @ContextCustomizerFactories(TestContextBootstrapperAdditionalTests.ValidFactory.class)
  static class Configured { }

  @infra.context.annotation.Configuration(proxyBeanMethods = false)
  static class Configuration { }

  static class Initializer implements ApplicationContextInitializer {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) { }
  }

  @infra.test.context.TestPropertySource(properties = "diagnostics=true")
  static class Properties { }

  @infra.test.context.TestPropertySource(properties = "diagnostics=true")
  static class DuplicateProperties extends Properties { }

}
