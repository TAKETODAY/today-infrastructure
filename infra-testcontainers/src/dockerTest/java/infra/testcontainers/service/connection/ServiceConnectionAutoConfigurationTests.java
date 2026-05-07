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

package infra.testcontainers.service.connection;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Set;

import javax.sql.DataSource;

import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.support.BeanNameGenerator;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.BootstrapContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.context.aot.ApplicationContextAotGenerator;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.type.AnnotationMetadata;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;
import infra.test.testcontainers.DisabledIfDockerUnavailable;
import infra.test.testcontainers.TestImage;
import infra.testcontainers.beans.TestcontainerBeanDefinition;
import infra.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnectionAutoConfiguration} and
 * {@link ServiceConnectionAutoConfigurationRegistrar}.
 *
 * @author Phillip Webb
 */
@DisabledIfDockerUnavailable
class ServiceConnectionAutoConfigurationTests {

  private static final String DATABASE_CONTAINER_CONNECTION_DETAILS = TestDatabaseConnectionDetails.class.getName();

  @Test
  void whenNoExistingBeansRegistersServiceConnection() {
    try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
      applicationContext.register(WithNoExtraAutoConfiguration.class, ContainerConfiguration.class);
      new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
      applicationContext.refresh();
      DatabaseConnectionDetails connectionDetails = applicationContext.getBean(DatabaseConnectionDetails.class);
      assertThat(connectionDetails.getClass().getName()).isEqualTo(DATABASE_CONTAINER_CONNECTION_DETAILS);
    }
  }

  @Test
  void whenHasExistingAutoConfigurationRegistersReplacement() {
    try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
      applicationContext.register(WithDatasourceConfiguration.class, ContainerConfiguration.class);
      new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
      applicationContext.refresh();
      DatabaseConnectionDetails connectionDetails = applicationContext.getBean(DatabaseConnectionDetails.class);
      assertThat(connectionDetails.getClass().getName()).isEqualTo(DATABASE_CONTAINER_CONNECTION_DETAILS);
    }
  }

  @Test
  void whenHasUserConfigurationDoesNotRegisterReplacement() {
    try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
      applicationContext.register(UserConfiguration.class, WithDatasourceConfiguration.class,
              ContainerConfiguration.class);
      new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
      applicationContext.refresh();
      DatabaseConnectionDetails connectionDetails = applicationContext.getBean(DatabaseConnectionDetails.class);
      assertThat(Mockito.mockingDetails(connectionDetails).isMock()).isTrue();
    }
  }

  @Test
  void whenHasTestcontainersBeanDefinition() {
    try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
      applicationContext.register(WithNoExtraAutoConfiguration.class,
              TestcontainerBeanDefinitionConfiguration.class);
      new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
      applicationContext.refresh();
      DatabaseConnectionDetails connectionDetails = applicationContext.getBean(DatabaseConnectionDetails.class);
      assertThat(connectionDetails.getClass().getName()).isEqualTo(DATABASE_CONTAINER_CONNECTION_DETAILS);
    }
  }

  @Test
  void serviceConnectionBeansDoNotCauseAotProcessingToFail() {
    try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
      applicationContext.register(WithNoExtraAutoConfiguration.class, ContainerConfiguration.class);
      new TestcontainersLifecycleApplicationContextInitializer().initialize(applicationContext);
      TestGenerationContext generationContext = new TestGenerationContext();
      assertThatNoException().isThrownBy(() -> new ApplicationContextAotGenerator()
              .processAheadOfTime(applicationContext, generationContext));
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration(ServiceConnectionAutoConfiguration.class)
  static class WithNoExtraAutoConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration(ServiceConnectionAutoConfiguration.class)
  static class WithDatasourceConfiguration {

    @Bean
    DataSource dataSource() {
      EmbeddedDatabaseFactory embeddedDatabaseFactory = new EmbeddedDatabaseFactory();
      embeddedDatabaseFactory.setGenerateUniqueDatabaseName(true);
      embeddedDatabaseFactory.setDatabaseType(EmbeddedDatabaseType.H2);
      return embeddedDatabaseFactory.getDatabase();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ContainerConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
      return TestImage.container(PostgreSQLContainer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class UserConfiguration {

    @Bean
    DatabaseConnectionDetails databaseConnectionDetails() {
      return mock(DatabaseConnectionDetails.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(TestcontainerBeanDefinitionRegistrar.class)
  static class TestcontainerBeanDefinitionConfiguration {

  }

  static class TestcontainerBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BootstrapContext context, BeanNameGenerator importBeanNameGenerator) {
      context.registerBeanDefinition("postgresContainer", new TestcontainersRootBeanDefinition());
    }

  }

  static class TestcontainersRootBeanDefinition extends RootBeanDefinition implements TestcontainerBeanDefinition {

    private final PostgreSQLContainer container = TestImage.container(PostgreSQLContainer.class);

    TestcontainersRootBeanDefinition() {
      setBeanClass(PostgreSQLContainer.class);
      setInstanceSupplier(() -> this.container);
    }

    @Override
    public String getContainerImageName() {
      return this.container.getDockerImageName();
    }

    @Override
    public MergedAnnotations getAnnotations() {
      MergedAnnotation<ServiceConnection> annotation = MergedAnnotation.valueOf(ServiceConnection.class);
      return MergedAnnotations.valueOf(Set.of(annotation));
    }

  }

}
