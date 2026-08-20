/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurationPackage;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code ImportsContextCustomizerFactory} when used with
 * {@link ImportAutoConfiguration @ImportAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ImportsContextCustomizerFactoryWithAutoConfigurationTests {

  static @Nullable ApplicationContext contextFromTest;

  @Test
  void testClassesThatHaveSameAnnotationsShareAContext() {
    executeTests(ExampleTest1.class);
    ApplicationContext test1Context = contextFromTest;
    assertThat(test1Context).isNotNull();
    executeTests(ExampleTest3.class);
    ApplicationContext test2Context = contextFromTest;
    assertThat(test2Context).isNotNull();
    assertThat(test1Context).isSameAs(test2Context);
  }

  @Test
  void testClassesThatOnlyHaveDifferingUnrelatedAnnotationsShareAContext() {
    executeTests(ExampleTest1.class);
    ApplicationContext test1Context = contextFromTest;
    assertThat(test1Context).isNotNull();
    executeTests(ExampleTest2.class);
    ApplicationContext test2Context = contextFromTest;
    assertThat(test2Context).isNotNull();
    assertThat(test1Context).isSameAs(test2Context);
  }

  @Test
  void testClassesThatOnlyHaveDifferingPropertyMappedAnnotationAttributesDoNotShareAContext() {
    executeTests(ExampleTest1.class);
    ApplicationContext test1Context = contextFromTest;
    assertThat(test1Context).isNotNull();
    executeTests(ExampleTest4.class);
    ApplicationContext test2Context = contextFromTest;
    assertThat(test2Context).isNotNull();
    assertThat(test1Context).isNotSameAs(test2Context);
  }

  private void executeTests(Class<?> testClass) {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(testClass))
            .build();
    Launcher launcher = LauncherFactory.create();
    launcher.execute(request);
  }

  @ExampleTest
  @ContextConfiguration(classes = EmptyConfig.class)
  @Unrelated1
  static class ExampleTest1 {

    @Autowired
    private ApplicationContext context;

    @Test
    void test() {
      contextFromTest = this.context;
    }

  }

  @ExampleTest
  @ContextConfiguration(classes = EmptyConfig.class)
  @Unrelated2
  static class ExampleTest2 {

    @Autowired
    private ApplicationContext context;

    @Test
    void test() {
      contextFromTest = this.context;
    }

  }

  @ExampleTest
  @ContextConfiguration(classes = EmptyConfig.class)
  @Unrelated1
  static class ExampleTest3 {

    @Autowired
    private ApplicationContext context;

    @Test
    void test() {
      contextFromTest = this.context;
    }

  }

  @ExampleTest(attribute = false)
  @ContextConfiguration(classes = EmptyConfig.class)
  @Unrelated1
  static class ExampleTest4 {

    @Autowired
    private ApplicationContext context;

    @Test
    void test() {
      contextFromTest = this.context;
    }

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Unrelated1 {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Unrelated2 {

  }

  @Configuration(proxyBeanMethods = false)
  @AutoConfigurationPackage
  static class EmptyConfig {

  }

}
