/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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