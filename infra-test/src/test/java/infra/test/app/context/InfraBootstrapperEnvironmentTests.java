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

package infra.test.app.context;

import org.junit.jupiter.api.Test;

import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextHierarchy;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.aot.AotTestAttributes;
import infra.test.context.support.DefaultBootstrapContext;
import infra.test.context.web.WebMergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InfraBootstrapperEnvironmentTests {

  @Test
  void explicitApplicationTypesControlMergedConfiguration() {
    InfraTestContextBootstrapper bootstrapper = bootstrapper();
    assertThat(bootstrapper.processMergedContextConfiguration(configuration("normal", NoEnvironment.class)))
            .isNotInstanceOf(WebMergedContextConfiguration.class)
            .isNotInstanceOf(ReactiveWebMergedContextConfiguration.class);
    assertThat(bootstrapper.processMergedContextConfiguration(configuration("reactive-web", MockEnvironment.class)))
            .isInstanceOf(ReactiveWebMergedContextConfiguration.class);
    assertThat(bootstrapper.processMergedContextConfiguration(configuration("web", MockEnvironment.class)))
            .isInstanceOf(WebMergedContextConfiguration.class);
    assertThat(bootstrapper.processMergedContextConfiguration(configuration("reactive-web", DefinedEnvironment.class)))
            .isInstanceOf(ReactiveWebMergedContextConfiguration.class);
  }

  @Test
  void modifiedConfigurationKeepsPropertiesAndReplacesClasses() {
    InfraTestContextBootstrapper bootstrapper = bootstrapper();
    MergedContextConfiguration original = configuration("normal", NoEnvironment.class);
    MergedContextConfiguration modified = bootstrapper.createModifiedConfig(original, new Class<?>[] { String.class });
    assertThat(modified.getClasses()).containsExactly(String.class);
    assertThat(modified.getPropertySourceProperties()).containsExactly(original.getPropertySourceProperties());
    assertThat(modified.getContextCustomizers()).isNotEmpty();
  }

  @Test
  void explicitClassesCanReplaceNullProcessedClasses() {
    InfraTestContextBootstrapper bootstrapper = bootstrapper();
    ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(ExplicitClasses.class);
    attributes.setClasses((Class<?>[]) null);
    bootstrapper.resolveContextLoader(ExplicitClasses.class, java.util.List.of(attributes));
    assertThat(attributes.getClasses()).containsExactly(Config.class);
  }

  @Test
  void unsupportedWebEnvironmentsKeepPlainMergedConfigurations() {
    var bootstrapper = bootstrapper();
    for (Class<?> type : new Class<?>[] { NoEnvironment.class, EmptyHierarchy.class }) {
      assertThat(bootstrapper.processMergedContextConfiguration(configuration("web", type)))
              .isNotInstanceOf(WebMergedContextConfiguration.class);
      assertThat(bootstrapper.processMergedContextConfiguration(configuration("reactive-web", type)))
              .isNotInstanceOf(ReactiveWebMergedContextConfiguration.class);
    }
    var withoutDifferentiator = new InfraTestContextBootstrapper(mock(AotTestAttributes.class)) {
      @Override
      protected String getDifferentiatorPropertySourceProperty() {
        return null;
      }
    };
    withoutDifferentiator.setBootstrapContext(new DefaultBootstrapContext(getClass(), mock(CacheAwareContextLoaderDelegate.class)));
    assertThat(withoutDifferentiator.processMergedContextConfiguration(configuration("normal", NoEnvironment.class))
            .getPropertySourceProperties()).containsExactly("app.main.application-type=normal");
  }

  private InfraTestContextBootstrapper bootstrapper() {
    InfraTestContextBootstrapper bootstrapper = new InfraTestContextBootstrapper(mock(AotTestAttributes.class));
    bootstrapper.setBootstrapContext(new DefaultBootstrapContext(getClass(), mock(CacheAwareContextLoaderDelegate.class)));
    return bootstrapper;
  }

  private MergedContextConfiguration configuration(String type, Class<?> testClass) {
    return new MergedContextConfiguration(testClass, null, new Class<?>[] { Config.class }, null, null,
            (String[]) null, new String[] { "app.main.application-type=" + type }, null, null, null);
  }

  static class NoEnvironment { }

  @ContextHierarchy({})
  @InfraTest(webEnvironment = InfraTest.WebEnvironment.NONE)
  static class EmptyHierarchy { }

  @InfraTest(webEnvironment = InfraTest.WebEnvironment.MOCK)
  static class MockEnvironment { }

  @InfraTest(webEnvironment = InfraTest.WebEnvironment.DEFINED_PORT)
  static class DefinedEnvironment { }

  @InfraTest(classes = Config.class)
  static class ExplicitClasses { }

  @infra.context.annotation.Configuration(proxyBeanMethods = false)
  static class Config { }

}
