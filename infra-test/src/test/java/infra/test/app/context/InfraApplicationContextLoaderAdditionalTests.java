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

import infra.app.InfraConfiguration;
import infra.app.ApplicationContextFactory;
import infra.app.ApplicationType;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.core.env.StandardEnvironment;
import infra.test.context.BootstrapUtils;
import infra.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class InfraApplicationContextLoaderAdditionalTests {

  @Test
  void mainMethodMustExistAndBeStaticWhenRequired() {
    var loader = new InfraApplicationContextLoader();
    assertThatIllegalStateException().isThrownBy(() -> loader.loadContext(configuration(NoInfraConfiguration.class)))
            .withMessageContaining("no @InfraConfiguration");
    assertThatIllegalStateException().isThrownBy(() -> loader.loadContext(configuration(NoMain.class)))
            .withMessageContaining("Main method not found");
    assertThatIllegalStateException().isThrownBy(() -> loader.loadContext(configuration(InstanceMain.class)))
            .withMessageContaining("Main method not found");
    assertThatIllegalStateException().isThrownBy(() -> loader.loadContext(configuration(EmptyMain.class)))
            .withMessage("No root application context located");
  }

  @Test
  void customEnvironmentReceivesProfilesAndInlineProperties() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    var loader = new InfraApplicationContextLoader() {
      @Override
      protected StandardEnvironment getEnvironment() {
        return environment;
      }
    };
    var config = new MergedContextConfiguration(getClass(), null, new Class<?>[] { Plain.class },
            null, new String[] { "coverage" }, null, new String[] { "coverage.value=present" }, loader, null, null);
    try (ConfigurableApplicationContext context = (ConfigurableApplicationContext) loader.loadContext(config)) {
      assertThat(context.getEnvironment()).isSameAs(environment);
      assertThat(environment.getActiveProfiles()).containsExactly("coverage");
      assertThat(environment.getProperty("coverage.value")).isEqualTo("present");
    }
  }

  @Test
  void aotRuntimeRequiresAnInitializerAndCreatesAnActiveContext() throws Exception {
    var loader = new InfraApplicationContextLoader();
    var config = new MergedContextConfiguration(getClass(), null, new Class<?>[] { Plain.class }, null, loader);
    try (ConfigurableApplicationContext context = (ConfigurableApplicationContext)
            loader.loadContextForAotRuntime(config, application -> application.getBeanFactory().registerSingleton("marker", "initialized"))) {
      assertThat(context.isActive()).isTrue();
      assertThat(context.getBean("marker")).isEqualTo("initialized");
    }
  }

  @Test
  void applicationContextFactorySelectsWebContextImplementations() {
    ExposedLoader loader = new ExposedLoader();
    MergedContextConfiguration config = configuration(Plain.class);
    ApplicationContextFactory factory = loader.applicationContextFactory(config);
    assertThat(factory.create(ApplicationType.NORMAL)).isInstanceOf(GenericApplicationContext.class);
    assertThat(factory.create(ApplicationType.WEB)).isInstanceOf(infra.web.mock.support.GenericMockWebApplicationContext.class);
    assertThat(factory.create(ApplicationType.REACTIVE_WEB))
            .isInstanceOf(infra.web.reactive.context.GenericReactiveWebApplicationContext.class);
  }

  private MergedContextConfiguration configuration(Class<?> type) {
    return BootstrapUtils.resolveTestContextBootstrapper(type).buildMergedContextConfiguration();
  }

  private static class ExposedLoader extends InfraApplicationContextLoader {
    ApplicationContextFactory applicationContextFactory(MergedContextConfiguration config) {
      return getApplicationContextFactory(config);
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class Plain { }

  @InfraTest(classes = Plain.class, useMainMethod = InfraTest.UseMainMethod.ALWAYS, webEnvironment = InfraTest.WebEnvironment.NONE)
  static class NoInfraConfiguration { }

  @InfraConfiguration(proxyBeanMethods = false)
  static class WithoutMain { }

  @InfraTest(classes = WithoutMain.class, useMainMethod = InfraTest.UseMainMethod.ALWAYS, webEnvironment = InfraTest.WebEnvironment.NONE)
  static class NoMain { }

  @InfraConfiguration(proxyBeanMethods = false)
  static class WithInstanceMain {
    void main(String[] args) { }
  }

  @InfraTest(classes = WithInstanceMain.class, useMainMethod = InfraTest.UseMainMethod.ALWAYS, webEnvironment = InfraTest.WebEnvironment.NONE)
  static class InstanceMain { }

  @InfraConfiguration(proxyBeanMethods = false)
  static class WithEmptyMain {
    static void main() { }
  }

  @InfraTest(classes = WithEmptyMain.class, useMainMethod = InfraTest.UseMainMethod.ALWAYS, webEnvironment = InfraTest.WebEnvironment.NONE)
  static class EmptyMain { }

}
