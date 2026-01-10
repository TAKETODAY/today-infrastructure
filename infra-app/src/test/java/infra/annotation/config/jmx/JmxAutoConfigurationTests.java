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

package infra.annotation.config.jmx;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.annotation.config.UserConfigurations;
import infra.jmx.export.MBeanExporter;
import infra.jmx.export.annotation.ManagedAttribute;
import infra.jmx.export.annotation.ManagedOperation;
import infra.jmx.export.annotation.ManagedResource;
import infra.jmx.export.naming.MetadataNamingStrategy;
import infra.jmx.export.naming.ObjectNamingStrategy;
import infra.jmx.support.RegistrationPolicy;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/9 19:21
 */
class JmxAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class));

  @Test
  void testDefaultMBeanExport() {
    this.contextRunner.run((context) -> {
      assertThat(context).doesNotHaveBean(MBeanExporter.class);
      assertThat(context).doesNotHaveBean(ObjectNamingStrategy.class);
    });
  }

  @Test
  void testDisabledMBeanExport() {
    this.contextRunner.withPropertyValues("infra.jmx.enabled=false").run((context) -> {
      assertThat(context).doesNotHaveBean(MBeanExporter.class);
      assertThat(context).doesNotHaveBean(ObjectNamingStrategy.class);
    });
  }

  @Test
  void testEnabledMBeanExport() {
    this.contextRunner.withPropertyValues("infra.jmx.enabled=true").run((context) -> {
      assertThat(context).hasSingleBean(MBeanExporter.class);
      assertThat(context).hasSingleBean(ParentAwareNamingStrategy.class);
      MBeanExporter exporter = context.getBean(MBeanExporter.class);
      assertThat(exporter).hasFieldOrPropertyWithValue("ensureUniqueRuntimeObjectNames", false);
      assertThat(exporter).hasFieldOrPropertyWithValue("registrationPolicy", RegistrationPolicy.FAIL_ON_EXISTING);

      MetadataNamingStrategy naming = (MetadataNamingStrategy) ReflectionTestUtils.getField(exporter,
              "namingStrategy");
      assertThat(naming).hasFieldOrPropertyWithValue("ensureUniqueRuntimeObjectNames", false);
    });
  }

  @Test
  void testDefaultDomainConfiguredOnMBeanExport() {
    this.contextRunner.withPropertyValues(
            "infra.jmx.enabled=true", "infra.jmx.default-domain=my-test-domain",
            "infra.jmx.unique-names=true", "infra.jmx.registration-policy=IGNORE_EXISTING").run((context) -> {
      assertThat(context).hasSingleBean(MBeanExporter.class);
      MBeanExporter exporter = context.getBean(MBeanExporter.class);
      assertThat(exporter).hasFieldOrPropertyWithValue(
              "ensureUniqueRuntimeObjectNames", true);
      assertThat(exporter).hasFieldOrPropertyWithValue(
              "registrationPolicy", RegistrationPolicy.IGNORE_EXISTING);
      MetadataNamingStrategy naming = (MetadataNamingStrategy) ReflectionTestUtils.getField(exporter,
              "namingStrategy");
      assertThat(naming).hasFieldOrPropertyWithValue("defaultDomain", "my-test-domain");
      assertThat(naming).hasFieldOrPropertyWithValue("ensureUniqueRuntimeObjectNames", true);
    });
  }

  @Test
  void testBasicParentContext() {
    AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
    parent.register(JmxAutoConfiguration.class);
    parent.refresh();
    this.contextRunner.withParent(parent).run((context) -> assertThat(context.isRunning()));

    parent.close();
  }

  @Test
  void testParentContext() {
    AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
    parent.register(JmxAutoConfiguration.class, TestConfiguration.class);
    parent.refresh();
    this.contextRunner.withParent(parent).withConfiguration(UserConfigurations.of(TestConfiguration.class))
            .run((context) -> assertThat(context.isRunning()));
    parent.close();
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    @Bean
    Counter counter() {
      return new Counter();
    }

  }

  @ManagedResource
  public static class Counter {

    private int counter = 0;

    @ManagedAttribute
    public int get() {
      return this.counter;
    }

    @ManagedOperation
    public void increment() {
      this.counter++;
    }

  }

}