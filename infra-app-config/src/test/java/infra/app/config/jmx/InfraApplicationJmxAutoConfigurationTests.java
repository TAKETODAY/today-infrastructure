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

package infra.app.config.jmx;

import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import infra.app.ApplicationType;
import infra.app.builder.ApplicationBuilder;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.jmx.export.MBeanExporter;
import infra.web.server.context.WebServerApplicationContext;
import infra.web.server.netty.RandomPortWebServerConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/23 21:04
 */
class InfraApplicationJmxAutoConfigurationTests {

  private static final String ENABLE_ADMIN_PROP = "app.admin.enabled=true";

  private static final String DEFAULT_JMX_NAME = "infra.app:type=Admin,name=InfraApplication";

  private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(infra.app.config.jmx.InfraApplicationJmxAutoConfiguration.class));

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
    try (ConfigurableApplicationContext context = new ApplicationBuilder().sources(RandomPortWebServerConfig.class,
                    MultipleMBeanExportersConfiguration.class, infra.app.config.jmx.InfraApplicationJmxAutoConfiguration.class)
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
            .sources(MultipleMBeanExportersConfiguration.class, infra.app.config.jmx.InfraApplicationJmxAutoConfiguration.class);
    ApplicationBuilder childBuilder = parentBuilder
            .child(MultipleMBeanExportersConfiguration.class, infra.app.config.jmx.InfraApplicationJmxAutoConfiguration.class)
            .type(ApplicationType.NORMAL);
    try (ConfigurableApplicationContext parent = parentBuilder.run("--" + ENABLE_ADMIN_PROP);
            ConfigurableApplicationContext child = childBuilder.run("--" + ENABLE_ADMIN_PROP)) {
      BeanFactoryUtils.beanOfType(parent.getBeanFactory(), infra.app.config.jmx.InfraApplicationMXBeanRegistrar.class);
      assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> BeanFactoryUtils
              .beanOfType(child.getBeanFactory(), infra.app.config.jmx.InfraApplicationMXBeanRegistrar.class));
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