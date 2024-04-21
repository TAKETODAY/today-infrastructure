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

package cn.taketoday.annotation.config.jmx;

import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import cn.taketoday.annotation.config.web.RandomPortWebServerConfig;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.builder.ApplicationBuilder;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.web.context.WebServerApplicationContext;
import cn.taketoday.jmx.export.MBeanExporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/23 21:04
 */
class InfraApplicationJmxAutoConfigurationTests {

  private static final String ENABLE_ADMIN_PROP = "app.admin.enabled=true";

  private static final String DEFAULT_JMX_NAME = "cn.taketoday.app:type=Admin,name=InfraApplication";

  private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(InfraApplicationJmxAutoConfiguration.class));

  @Test
  void notRegisteredWhenThereAreNoMBeanExporter() {
    this.contextRunner.withPropertyValues(ENABLE_ADMIN_PROP).run((context) -> {
      ObjectName objectName = createDefaultObjectName();
      ObjectInstance objectInstance = this.server.getObjectInstance(objectName);
      assertThat(objectInstance).as("Lifecycle bean should have been registered").isNotNull();
    });
  }

  @Test
  void notRegisteredByDefaultWhenThereAreMultipleMBeanExporters() {
    this.contextRunner.withUserConfiguration(MultipleMBeanExportersConfiguration.class)
            .run((context) -> assertThatExceptionOfType(InstanceNotFoundException.class)
                    .isThrownBy(() -> this.server.getObjectInstance(createDefaultObjectName())));
  }

  @Test
  void registeredWithPropertyWhenThereAreMultipleMBeanExporters() {
    this.contextRunner.withUserConfiguration(MultipleMBeanExportersConfiguration.class)
            .withPropertyValues(ENABLE_ADMIN_PROP)
            .run((context) -> {
              ObjectName objectName = createDefaultObjectName();
              ObjectInstance objectInstance = this.server.getObjectInstance(objectName);
              assertThat(objectInstance).as("Lifecycle bean should have been registered").isNotNull();
            });
  }

  @Test
  void registerWithCustomJmxNameWhenThereAreMultipleMBeanExporters() {
    String customJmxName = "org.acme:name=FooBar";
    this.contextRunner.withUserConfiguration(MultipleMBeanExportersConfiguration.class)
            .withSystemProperties("app.admin.jmx-name=" + customJmxName)
            .withPropertyValues(ENABLE_ADMIN_PROP)
            .run((context) -> {
              try {
                this.server.getObjectInstance(createObjectName(customJmxName));
              }
              catch (InstanceNotFoundException ex) {
                fail("Admin MBean should have been exposed with custom name");
              }
              assertThatExceptionOfType(InstanceNotFoundException.class)
                      .isThrownBy(() -> this.server.getObjectInstance(createDefaultObjectName()));
            });
  }

  @Test
  void registerWithSimpleWebApp() throws Exception {
    try (ConfigurableApplicationContext context = new ApplicationBuilder()
            .sources(RandomPortWebServerConfig.class,
                    MultipleMBeanExportersConfiguration.class, InfraApplicationJmxAutoConfiguration.class)
            .run("--" + ENABLE_ADMIN_PROP)) {
      assertThat(context).isInstanceOf(WebServerApplicationContext.class);
      assertThat(this.server.getAttribute(createDefaultObjectName(), "EmbeddedWebApplication"))
              .isEqualTo(Boolean.TRUE);
      int expected = ((WebServerApplicationContext) context).getWebServer().getPort();
      String actual = getProperty(createDefaultObjectName(), "local.server.port");
      assertThat(actual).isEqualTo(String.valueOf(expected));
    }
  }

  @Test
  void onlyRegisteredOnceWhenThereIsAChildContext() {
    ApplicationBuilder parentBuilder = new ApplicationBuilder().type(ApplicationType.NORMAL)
            .sources(MultipleMBeanExportersConfiguration.class, InfraApplicationJmxAutoConfiguration.class);
    ApplicationBuilder childBuilder = parentBuilder
            .child(MultipleMBeanExportersConfiguration.class, InfraApplicationJmxAutoConfiguration.class)
            .type(ApplicationType.NORMAL);
    try (ConfigurableApplicationContext parent = parentBuilder.run("--" + ENABLE_ADMIN_PROP);
            ConfigurableApplicationContext child = childBuilder.run("--" + ENABLE_ADMIN_PROP)) {
      BeanFactoryUtils.beanOfType(parent.getBeanFactory(), InfraApplicationMXBeanRegistrar.class);
      assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> BeanFactoryUtils
              .beanOfType(child.getBeanFactory(), InfraApplicationMXBeanRegistrar.class));
    }
  }

  private ObjectName createDefaultObjectName() {
    return createObjectName(DEFAULT_JMX_NAME);
  }

  private ObjectName createObjectName(String jmxName) {
    try {
      return new ObjectName(jmxName);
    }
    catch (MalformedObjectNameException ex) {
      throw new IllegalStateException("Invalid jmx name " + jmxName, ex);
    }
  }

  private String getProperty(ObjectName objectName, String key) throws Exception {
    return (String) this.server.invoke(objectName, "getProperty", new Object[] { key },
            new String[] { String.class.getName() });
  }

  @Configuration(proxyBeanMethods = false)
  static class MultipleMBeanExportersConfiguration {

    @Bean
    MBeanExporter firstMBeanExporter() {
      return new MBeanExporter();
    }

    @Bean
    MBeanExporter secondMBeanExporter() {
      return new MBeanExporter();
    }

  }

}